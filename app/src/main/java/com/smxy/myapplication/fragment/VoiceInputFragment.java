package com.smxy.myapplication.fragment;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.smxy.myapplication.R;
import com.smxy.myapplication.activity.MainContainerActivity;
import com.smxy.myapplication.manager.FuzzyMatchHelper;
import com.smxy.myapplication.manager.TextToSpeechHelper;
import com.smxy.myapplication.manager.LayoutManager;
import com.smxy.myapplication.utils.ErrorHandler;

import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VoiceInputFragment extends Fragment {

    private static final String TAG = "VoiceInputFragment";
    private static final int SAMPLE_RATE = 16000;
    private static final int RECOGNITION_TIMEOUT_MS = 10000;
    private static final int SILENCE_TIMEOUT_MS = 3000;

    private TextView tvResult, tvVolume, tvParseResult;
    private ProgressBar volumeBar;
    private Button btnStartRecord, btnParse, btnBack, btnVoiceCommand;

    private String recognizedText = "";
    private boolean hasResult = false;
    private boolean isModelReady = false;

    private Model voskModel;
    private ExecutorService executorService;

    private AudioRecord audioRecord;
    private volatile boolean isRecording = false;
    private Thread recognitionThread;
    private Handler mainHandler;
    private Runnable timeoutRunnable;
    private long lastSoundTime = 0;

    private Map<String, String> cuisineMap = new HashMap<>();
    private Map<String, String> recipeActionMap = new HashMap<>();
    private List<String> cookingMethods = new ArrayList<>();

    private TextToSpeechHelper ttsHelper;
    private boolean enableVoiceOutput = true;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted && ErrorHandler.isContextValid(getContext())) {
                    Toast.makeText(getContext(), "录音权限已获取", Toast.LENGTH_SHORT).show();
                } else if (ErrorHandler.isContextValid(getContext())) {
                    Toast.makeText(getContext(), "录音权限被拒绝，无法使用语音功能", Toast.LENGTH_SHORT).show();
                }
            }
    );

    private final ActivityResultLauncher<String> requestRecordPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (!isGranted && ErrorHandler.isContextValid(getContext())) {
                    Toast.makeText(getContext(), "需要录音权限才能使用语音识别", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_voice_input, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (!ErrorHandler.isFragmentValid(this)) return;

        initViews(view);
        initKeywordMaps();
        setupListeners();
        checkRecordPermission();

        mainHandler = new Handler(Looper.getMainLooper());
        executorService = Executors.newSingleThreadExecutor();

        if (ErrorHandler.isContextValid(getContext())) {
            ttsHelper = TextToSpeechHelper.getInstance(requireContext());
            ttsHelper.setOnSpeakListener(new TextToSpeechHelper.OnSpeakListener() {
                @Override
                public void onStart(String utteranceId) {
                    Log.d(TAG, "开始播报: " + utteranceId);
                }

                @Override
                public void onDone(String utteranceId) {
                    Log.d(TAG, "播报完成: " + utteranceId);
                }

                @Override
                public void onError(String utteranceId) {
                    Log.e(TAG, "播报错误: " + utteranceId);
                }
            });
        }

        initVoskModelAsync();
    }

    private void initViews(View view) {
        btnStartRecord = view.findViewById(R.id.btn_start_record);
        tvResult = view.findViewById(R.id.tv_result);
        tvVolume = view.findViewById(R.id.tv_volume);
        volumeBar = view.findViewById(R.id.volume_bar);
        btnParse = view.findViewById(R.id.btn_parse);
        tvParseResult = view.findViewById(R.id.tv_parse_result);
        btnBack = view.findViewById(R.id.btn_back);
        btnVoiceCommand = view.findViewById(R.id.btn_voice_command);

        if (btnStartRecord != null) {
            btnStartRecord.setEnabled(false);
            btnStartRecord.setText("模型加载中...");
        }
    }

    private void initKeywordMaps() {
        cuisineMap.put("川菜", "川菜特点：麻辣鲜香，代表菜有麻婆豆腐、宫保鸡丁、水煮鱼");
        cuisineMap.put("粤菜", "粤菜特点：清鲜嫩滑，代表菜有白切鸡、叉烧、蒸鱼");
        cuisineMap.put("苏菜", "苏菜特点：口味平和，代表菜有松鼠桂鱼、清炖蟹粉狮子头");
        cuisineMap.put("浙菜", "浙菜特点：鲜美脆嫩，代表菜有西湖醋鱼、东坡肉");
        cuisineMap.put("闽菜", "闽菜特点：清淡鲜香，代表菜有佛跳墙、荔枝肉");
        cuisineMap.put("湘菜", "湘菜特点：香辣浓郁，代表菜有剁椒鱼头、小炒黄牛肉");
        cuisineMap.put("徽菜", "徽菜特点：重油重色，代表菜有臭鳜鱼、毛豆腐");
        cuisineMap.put("鲁菜", "鲁菜特点：咸鲜纯正，代表菜有葱烧海参、糖醋鲤鱼");
        cuisineMap.put("法餐", "法餐特点：精致浪漫，代表菜有鹅肝、红酒炖牛肉、马卡龙");
        cuisineMap.put("意餐", "意餐特点：浓郁醇厚，代表菜有披萨、意面、提拉米苏");
        cuisineMap.put("日料", "日料特点：清淡精致，代表菜有寿司、刺身、天妇罗");
        cuisineMap.put("韩餐", "韩餐特点：辛辣开胃，代表菜有泡菜、烤肉、拌饭");
        cuisineMap.put("泰餐", "泰餐特点：酸辣香甜，代表菜有冬阴功汤、绿咖喱");
        cuisineMap.put("美餐", "美餐特点：量大实惠，代表菜有汉堡、牛排、炸鸡");
        cuisineMap.put("墨西哥菜", "墨西哥菜特点：酸辣浓郁，代表菜有塔可、牛油果酱");

        cookingMethods.add("炒");
        cookingMethods.add("煮");
        cookingMethods.add("炖");
        cookingMethods.add("蒸");
        cookingMethods.add("炸");
        cookingMethods.add("煎");
        cookingMethods.add("烤");
        cookingMethods.add("焖");
        cookingMethods.add("烧");
        cookingMethods.add("卤");
        cookingMethods.add("拌");
        cookingMethods.add("炝");

        recipeActionMap.put("搜索", "search");
        recipeActionMap.put("查找", "search");
        recipeActionMap.put("看看", "search");
        recipeActionMap.put("显示", "search");
        recipeActionMap.put("查看", "search");
        recipeActionMap.put("收藏", "favorite");
        recipeActionMap.put("喜欢", "favorite");
        recipeActionMap.put("加入收藏", "favorite");
        recipeActionMap.put("添加", "add");
        recipeActionMap.put("创建", "add");
        recipeActionMap.put("制作", "add");
        recipeActionMap.put("开始做", "cook");
        recipeActionMap.put("怎么做", "cook");
        recipeActionMap.put("步骤", "cook");
        recipeActionMap.put("制作步骤", "cook");
    }

    private void setupListeners() {
        if (btnStartRecord != null) {
            btnStartRecord.setOnClickListener(v -> {
                if (!ErrorHandler.isContextValid(getContext())) return;
                if (!isModelReady) {
                    Toast.makeText(getContext(), "语音模型未就绪，请稍后再试", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestRecordPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
                    return;
                }
                startVoiceRecognition();
            });
        }

        if (btnParse != null) {
            btnParse.setOnClickListener(v -> {
                if (!recognizedText.isEmpty()) {
                    parseVoiceCommand(recognizedText);
                } else if (ErrorHandler.isContextValid(getContext())) {
                    Toast.makeText(getContext(), "没有可解析的文本", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnVoiceCommand != null) {
            btnVoiceCommand.setOnClickListener(v -> showVoiceCommandDialog());
        }

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getActivity() != null) {
                    requireActivity().getOnBackPressedDispatcher().onBackPressed();
                }
            });
        }
    }

    private void showVoiceCommandDialog() {
        if (!ErrorHandler.isFragmentValid(this)) return;

        String[] commands = {
                "语音命令示例：",
                "",
                "📖 菜系查询：",
                "  • \"川菜有什么特点\"",
                "  • \"介绍一下粤菜\"",
                "  • \"西餐有哪些分类\"",
                "",
                "🔍 搜索菜谱：",
                "  • \"搜索番茄炒蛋\"",
                "  • \"找红烧肉的做法\"",
                "",
                "❤️ 收藏操作：",
                "  • \"收藏这个菜谱\"",
                "  • \"取消收藏\"",
                "",
                "➕ 添加菜谱：",
                "  • \"添加一个新菜谱\"",
                "",
                "👨‍🍳 烹饪步骤：",
                "  • \"宫保鸡丁怎么做\"",
                "  • \"查看酸辣土豆丝步骤\""
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("🎤 语音命令帮助")
                .setItems(commands, null)
                .setPositiveButton("知道了", null)
                .show();
    }

    private void checkRecordPermission() {
        if (!ErrorHandler.isContextValid(getContext())) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        }
    }

    private void initVoskModelAsync() {
        if (!ErrorHandler.isContextValid(getContext())) return;

        executorService.execute(() -> {
            try {
                String modelAssetPath = "vosk-model-small-cn-0.22";
                File modelDir = new File(requireContext().getFilesDir(), modelAssetPath);

                if (!modelDir.exists()) {
                    mainHandler.post(() -> {
                        if (ErrorHandler.isContextValid(getContext())) {
                            Toast.makeText(getContext(), "正在准备语音模型，请稍候...", Toast.LENGTH_SHORT).show();
                        }
                    });
                    copyAssetFolder(modelAssetPath, modelDir);
                    mainHandler.post(() -> {
                        if (ErrorHandler.isContextValid(getContext())) {
                            Toast.makeText(getContext(), "模型准备完毕", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                voskModel = new Model(modelDir.getAbsolutePath());
                isModelReady = true;

                mainHandler.post(() -> {
                    if (ErrorHandler.isFragmentValid(this) && btnStartRecord != null && ErrorHandler.isContextValid(getContext())) {
                        btnStartRecord.setEnabled(true);
                        btnStartRecord.setText("🎤 开始语音识别");
                        Toast.makeText(getContext(), "语音模型已就绪", Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (IOException e) {
                Log.e(TAG, "加载Vosk模型失败", e);
                mainHandler.post(() -> {
                    if (ErrorHandler.isFragmentValid(this) && btnStartRecord != null && ErrorHandler.isContextValid(getContext())) {
                        btnStartRecord.setEnabled(false);
                        btnStartRecord.setText("模型加载失败");
                        Toast.makeText(getContext(), "语音模型加载失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    private void copyAssetFolder(String assetPath, File targetDir) throws IOException {
        if (!ErrorHandler.isContextValid(getContext())) return;

        String[] files = requireContext().getAssets().list(assetPath);
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
        if (!ErrorHandler.isContextValid(getContext())) return;
        if (targetFile.exists()) return;

        try (InputStream is = requireContext().getAssets().open(assetPath);
             OutputStream os = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startVoiceRecognition() {
        if (!ErrorHandler.isContextValid(getContext())) return;
        if (!isModelReady || voskModel == null) {
            Toast.makeText(getContext(), "语音模型未就绪，请稍后再试", Toast.LENGTH_SHORT).show();
            return;
        }

        stopVoiceRecognition();

        hasResult = false;
        lastSoundTime = System.currentTimeMillis();

        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        if (bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            Toast.makeText(getContext(), "音频参数配置错误", Toast.LENGTH_SHORT).show();
            return;
        }

        int recordBufferSize = bufferSize * 2;
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                recordBufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Toast.makeText(getContext(), "麦克风初始化失败", Toast.LENGTH_SHORT).show();
            return;
        }

        Recognizer voskRecognizer;
        try {
            voskRecognizer = new Recognizer(voskModel, SAMPLE_RATE);
        } catch (IOException e) {
            Log.e(TAG, "创建Recognizer失败", e);
            Toast.makeText(getContext(), "语音识别器初始化失败", Toast.LENGTH_SHORT).show();
            return;
        }

        final Recognizer finalRecognizer = voskRecognizer;
        audioRecord.startRecording();
        isRecording = true;

        if (tvResult != null) {
            tvResult.setText("正在倾听...");
            tvResult.setAlpha(0.7f);
        }
        if (tvVolume != null) tvVolume.setText("正在录音...");
        if (volumeBar != null) volumeBar.setProgress(50);

        timeoutRunnable = () -> {
            if (isRecording && !hasResult && ErrorHandler.isContextValid(getContext())) {
                Toast.makeText(getContext(), "未检测到语音输入，已停止", Toast.LENGTH_SHORT).show();
                stopVoiceRecognition();
            }
        };
        mainHandler.postDelayed(timeoutRunnable, RECOGNITION_TIMEOUT_MS);

        recognitionThread = new Thread(() -> {
            byte[] buffer = new byte[bufferSize];
            boolean hasPartialResult = false;

            while (isRecording && !hasResult) {
                try {
                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);
                    if (bytesRead > 0) {
                        double rms = 0;
                        for (int i = 0; i < bytesRead; i += 2) {
                            if (i + 1 < bytesRead) {
                                short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                                rms += sample * sample;
                            }
                        }
                        rms = Math.sqrt(rms / (bytesRead / 2));
                        final int finalVolume = (int) Math.min(100, rms / 300);
                        mainHandler.post(() -> {
                            if (volumeBar != null) volumeBar.setProgress(finalVolume);
                            if (tvVolume != null) tvVolume.setText("音量: " + finalVolume + "%");
                        });

                        if (rms > 500) {
                            lastSoundTime = System.currentTimeMillis();
                        }

                        if (finalRecognizer.acceptWaveForm(buffer, bytesRead)) {
                            String resultJson = finalRecognizer.getResult();
                            handleRecognitionResult(resultJson);
                            break;
                        } else {
                            String partialJson = finalRecognizer.getPartialResult();
                            if (partialJson != null && !partialJson.equals("{\"partial\":\"\"}")) {
                                try {
                                    org.json.JSONObject jsonObject = new org.json.JSONObject(partialJson);
                                    String text = jsonObject.optString("partial", "");
                                    if (!text.isEmpty()) {
                                        hasPartialResult = true;
                                        updatePartialResult(text);
                                    }
                                } catch (org.json.JSONException e) {
                                    // 忽略
                                }
                            }
                        }

                        if (hasPartialResult && System.currentTimeMillis() - lastSoundTime > SILENCE_TIMEOUT_MS) {
                            mainHandler.post(() -> {
                                if (ErrorHandler.isContextValid(getContext())) {
                                    Toast.makeText(getContext(), "检测到静音，已停止", Toast.LENGTH_SHORT).show();
                                }
                            });
                            break;
                        }
                    } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION ||
                            bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e(TAG, "AudioRecord.read 错误: " + bytesRead);
                        break;
                    }
                    Thread.yield();
                } catch (Exception e) {
                    Log.e(TAG, "录音循环异常", e);
                    break;
                }
            }

            cleanupRecording(finalRecognizer);
        });
        recognitionThread.start();
    }

    private void updatePartialResult(String text) {
        mainHandler.post(() -> {
            if (tvResult != null) {
                tvResult.setText(text);
                tvResult.setAlpha(0.7f);
            }
        });
    }

    private void cleanupRecording(Recognizer recognizer) {
        synchronized (this) {
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
        }

        try {
            recognizer.close();
        } catch (Exception e) {
            Log.e(TAG, "关闭Recognizer异常", e);
        }

        mainHandler.post(() -> {
            if (!hasResult && tvResult != null) {
                tvResult.setText("未识别到语音");
                tvResult.setAlpha(1.0f);
            }
            if (tvVolume != null) tvVolume.setText(getString(R.string.volume_stopped));
            if (volumeBar != null) volumeBar.setProgress(0);
        });
    }

    private void handleRecognitionResult(String json) {
        if (getActivity() == null) return;
        try {
            org.json.JSONObject jsonObject = new org.json.JSONObject(json);
            String text = jsonObject.getString("text");
            if (text.isEmpty()) return;

            hasResult = true;
            recognizedText = text;

            mainHandler.post(() -> {
                if (tvResult != null) {
                    tvResult.setText(recognizedText);
                    tvResult.setAlpha(1.0f);
                }
                if (btnParse != null) btnParse.setEnabled(true);
                parseVoiceCommand(recognizedText);
            });

            stopVoiceRecognition();

        } catch (org.json.JSONException e) {
            Log.d(TAG, "解析结果（可忽略）: " + e.getMessage());
        }
    }

    private void stopVoiceRecognition() {
        if (timeoutRunnable != null) {
            mainHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }

        if (isRecording) {
            isRecording = false;
            if (recognitionThread != null) {
                try {
                    recognitionThread.join(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                recognitionThread = null;
            }
        }
    }

    private void parseVoiceCommand(String text) {
        if (!ErrorHandler.isFragmentValid(this)) return;

        String correctedText = FuzzyMatchHelper.correctTextEnhanced(text);
        String lowerText = correctedText.toLowerCase();

        StringBuilder result = new StringBuilder();
        boolean hasMatched = false;
        String speakText = null;

        for (Map.Entry<String, String> entry : cuisineMap.entrySet()) {
            String cuisineKey = entry.getKey().toLowerCase();
            if (lowerText.contains(cuisineKey)) {
                result.append("🍳 ").append(entry.getKey()).append("介绍：\n");
                result.append(entry.getValue()).append("\n\n");
                hasMatched = true;
                speakText = entry.getKey() + "，" + entry.getValue();
                break;
            }
        }

        if (!hasMatched) {
            for (Map.Entry<String, String> entry : recipeActionMap.entrySet()) {
                if (lowerText.contains(entry.getKey().toLowerCase())) {
                    String action = entry.getValue();
                    switch (action) {
                        case "search":
                            String searchKeyword = extractSearchKeyword(correctedText);
                            if (!searchKeyword.isEmpty()) {
                                result.append("🔍 搜索菜谱: \"").append(searchKeyword).append("\"\n");
                                result.append("即将为您搜索相关菜谱...");
                                performSearch(searchKeyword);
                                speakText = "正在搜索" + searchKeyword;
                            } else {
                                result.append("🔍 请输入要搜索的菜谱名称");
                                speakText = "请输入要搜索的菜谱名称";
                            }
                            hasMatched = true;
                            break;
                        case "favorite":
                            result.append("❤️ 已添加当前菜谱到收藏夹");
                            performFavorite();
                            speakText = "已添加当前菜谱到收藏夹";
                            hasMatched = true;
                            break;
                        case "add":
                            result.append("➕ 打开添加菜谱页面");
                            openAddRecipe();
                            speakText = "打开添加菜谱页面";
                            hasMatched = true;
                            break;
                        case "cook":
                            String recipeName = extractRecipeName(correctedText);
                            if (!recipeName.isEmpty()) {
                                result.append("👨‍🍳 查看菜谱: \"").append(recipeName).append("\" 的制作步骤");
                                showRecipeSteps(recipeName);
                                speakText = "正在查看" + recipeName + "的制作步骤";
                            } else {
                                result.append("👨‍🍳 请告诉我您想做什么菜");
                                speakText = "请告诉我您想做什么菜";
                            }
                            hasMatched = true;
                            break;
                    }
                    break;
                }
            }
        }

        if (!hasMatched) {
            for (String method : cookingMethods) {
                if (lowerText.contains(method) ||
                        lowerText.contains(method + "菜") ||
                        lowerText.contains(method + "的方法")) {
                    result.append("🍳 ").append(method).append("菜的技巧：\n");
                    result.append(getCookingMethodTip(method));
                    speakText = method + "菜的技巧是，" + getCookingMethodTip(method);
                    hasMatched = true;
                    break;
                }
            }
        }

        if (!hasMatched) {
            String helpText = "识别结果：" + correctedText + "\n\n" +
                    "💡 您可以尝试：\n" +
                    "• \"川菜有什么特点\"\n" +
                    "• \"搜索番茄炒蛋\"\n" +
                    "• \"宫保鸡丁怎么做\"\n" +
                    "• \"收藏这个菜谱\"\n" +
                    "• \"土豆\" (英文/拼音也支持)";
            result.append(helpText);
            speakText = "没有找到相关菜谱，您可以尝试说川菜有什么特点，或者搜索番茄炒蛋";
        }

        if (tvParseResult != null) {
            tvParseResult.setText(result.toString());
        }

        if (enableVoiceOutput && ttsHelper != null && ttsHelper.isInitialized() && speakText != null) {
            ttsHelper.speak(speakText, "voice_command");
        }
    }

    private String extractSearchKeyword(String text) {
        String keyword = text.replaceAll("搜索|查找|看看|显示|查看", "");
        keyword = keyword.replaceAll("的|菜谱|做法|步骤", "");
        return keyword.trim();
    }

    private String extractRecipeName(String text) {
        String recipeName = text.replaceAll("怎么做|步骤|如何做|制作|查看", "");
        recipeName = recipeName.replaceAll("的|菜|菜谱", "");
        return recipeName.trim();
    }

    private String getCookingMethodTip(String method) {
        Map<String, String> tips = new HashMap<>();
        tips.put("炒", "大火快炒，锁住食材水分，保持脆嫩口感");
        tips.put("煮", "水开后下锅，注意火候和时间控制");
        tips.put("炖", "小火慢炖，让食材充分入味，汤汁浓郁");
        tips.put("蒸", "保留食材原汁原味，营养不流失");
        tips.put("炸", "油温控制很重要，一般6-7成热下锅");
        tips.put("煎", "小火慢煎，一面煎至金黄再翻面");
        tips.put("烤", "提前预热烤箱，中途可翻面确保均匀");
        tips.put("焖", "小火焖煮，让食材充分吸收汤汁");
        tips.put("烧", "先炒香调料，再加入食材烧制入味");
        tips.put("卤", "卤水要老，香料要足，卤制时间要充分");
        tips.put("拌", "调味汁要调好，现拌现吃最美味");
        tips.put("炝", "热油爆香调料，快速翻炒出锅");
        return tips.getOrDefault(method, "掌握好火候和时间，多练习就能做出美味");
    }

    private void performSearch(String keyword) {
        if (getActivity() instanceof MainContainerActivity && ErrorHandler.isContextValid(getContext())) {
            ((MainContainerActivity) getActivity()).searchRecipe(keyword);
            Toast.makeText(getContext(), "搜索: " + keyword, Toast.LENGTH_SHORT).show();
        }
    }

    private void performFavorite() {
        if (getActivity() instanceof MainContainerActivity && ErrorHandler.isContextValid(getContext())) {
            ((MainContainerActivity) getActivity()).favoriteCurrentRecipe();
        }
    }

    private void openAddRecipe() {
        if (getActivity() instanceof MainContainerActivity && ErrorHandler.isContextValid(getContext())) {
            ((MainContainerActivity) getActivity()).openAddRecipe();
        }
    }

    private void showRecipeSteps(String recipeName) {
        if (getActivity() instanceof MainContainerActivity && ErrorHandler.isContextValid(getContext())) {
            ((MainContainerActivity) getActivity()).showRecipeSteps(recipeName);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (timeoutRunnable != null) {
            mainHandler.removeCallbacks(timeoutRunnable);
        }
        stopVoiceRecognition();
        if (voskModel != null) {
            voskModel.close();
            voskModel = null;
        }
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}