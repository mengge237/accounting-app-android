package com.smxy.myapplication.manager;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceRecognitionManager {
    private static final String TAG = "VoiceRecognitionManager";
    private static final int SAMPLE_RATE = 16000;

    private static VoiceRecognitionManager instance;
    private final Context context;
    private Model voskModel;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private boolean isModelReady = false;
    private boolean isListening = false;
    private AudioRecord audioRecord;
    private Thread recognitionThread;
    private String currentPartialText = "";

    public interface RecognitionCallback {
        void onPartialResult(String text);
        void onFinalResult(String text);
        void onError(String error);
    }

    private RecognitionCallback callback;

    private VoiceRecognitionManager(Context context) {
        this.context = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        initModel();
    }

    public static synchronized VoiceRecognitionManager getInstance(Context context) {
        if (context == null) {
            return null;
        }
        if (instance == null) {
            instance = new VoiceRecognitionManager(context);
        }
        return instance;
    }

    private void initModel() {
        executorService.execute(() -> {
            try {
                String modelAssetPath = "vosk-model-small-cn-0.22";
                File modelDir = new File(context.getFilesDir(), modelAssetPath);

                if (!modelDir.exists()) {
                    Log.d(TAG, "模型不存在，开始复制...");
                    copyAssetFolder(modelAssetPath, modelDir);
                    Log.d(TAG, "模型复制完成");
                }

                voskModel = new Model(modelDir.getAbsolutePath());
                isModelReady = true;
                Log.i(TAG, "Vosk模型加载成功");

            } catch (IOException e) {
                Log.e(TAG, "加载Vosk模型失败", e);
                isModelReady = false;
            }
        });
    }

    private void copyAssetFolder(String assetPath, File targetDir) throws IOException {
        String[] files = context.getAssets().list(assetPath);
        if (files == null || files.length == 0) {
            copyAssetFile(assetPath, targetDir);
            return;
        }
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new IOException("无法创建目标文件夹: " + targetDir.getAbsolutePath());
        }
        for (String file : files) {
            copyAssetFolder(assetPath + "/" + file, new File(targetDir, file));
        }
    }

    private void copyAssetFile(String assetPath, File targetFile) throws IOException {
        if (targetFile.exists()) return;
        try (InputStream is = context.getAssets().open(assetPath);
             OutputStream os = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    @SuppressLint("MissingPermission")
    public void startListening(RecognitionCallback callback) {
        this.callback = callback;
        this.currentPartialText = "";

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "没有录音权限");
            if (callback != null) {
                callback.onError("没有录音权限");
            }
            return;
        }

        startListeningInternal();
    }

    @SuppressLint("MissingPermission")
    private void startListeningInternal() {
        if (!isModelReady || voskModel == null) {
            Log.e(TAG, "模型未就绪");
            if (callback != null) {
                callback.onError("模型未就绪");
            }
            return;
        }

        if (isListening) {
            stopListening();
        }

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "音频参数错误");
            if (callback != null) {
                callback.onError("音频参数错误");
            }
            return;
        }

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize * 2);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "麦克风初始化失败");
            if (callback != null) {
                callback.onError("麦克风初始化失败");
            }
            return;
        }

        Recognizer recognizer;
        try {
            recognizer = new Recognizer(voskModel, SAMPLE_RATE);
        } catch (IOException e) {
            Log.e(TAG, "创建Recognizer失败", e);
            if (callback != null) {
                callback.onError("识别器初始化失败");
            }
            return;
        }

        audioRecord.startRecording();
        isListening = true;
        Log.d(TAG, "开始录音识别...");

        final Recognizer finalRecognizer = recognizer;
        recognitionThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            boolean gotFinal = false;

            while (isListening && !gotFinal) {
                try {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        if (finalRecognizer.acceptWaveForm(buffer, bytesRead)) {
                            String json = finalRecognizer.getResult();
                            String text = parseResult(json);
                            Log.d(TAG, "最终识别结果: " + text);
                            if (!text.isEmpty()) {
                                final String finalText = text;
                                mainHandler.post(() -> {
                                    if (callback != null) {
                                        callback.onFinalResult(finalText);
                                    }
                                });
                                gotFinal = true;
                            }
                        } else {
                            String partialJson = finalRecognizer.getPartialResult();
                            String partialText = parsePartialResult(partialJson);
                            if (!partialText.isEmpty() && !partialText.equals(currentPartialText)) {
                                currentPartialText = partialText;
                                Log.d(TAG, "临时识别: " + partialText);
                                final String tempText = partialText;
                                mainHandler.post(() -> {
                                    if (callback != null) {
                                        callback.onPartialResult(tempText);
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "录音异常", e);
                    break;
                }
            }

            cleanup(finalRecognizer);

            if (!gotFinal) {
                mainHandler.post(() -> {
                    if (callback != null) {
                        callback.onError("未识别到语音");
                    }
                });
            }
        });
        recognitionThread.start();
    }

    private String parseResult(String json) {
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(json);
            String text = jsonObject.optString("text", "");
            return text.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private String parsePartialResult(String json) {
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(json);
            String text = jsonObject.optString("partial", "");
            return text.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void cleanup(Recognizer recognizer) {
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "释放AudioRecord异常", e);
            }
            audioRecord = null;
        }

        try {
            recognizer.close();
        } catch (Exception e) {
            Log.e(TAG, "关闭Recognizer异常", e);
        }

        isListening = false;
        Log.d(TAG, "录音识别结束");
    }

    public void stopListening() {
        Log.d(TAG, "停止录音识别");
        isListening = false;
        if (recognitionThread != null) {
            try {
                recognitionThread.interrupt();
            } catch (Exception e) {
                // ignore
            }
            recognitionThread = null;
        }
    }

    public void destroy() {
        Log.d(TAG, "销毁识别器");
        stopListening();
        if (voskModel != null) {
            voskModel.close();
            voskModel = null;
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}