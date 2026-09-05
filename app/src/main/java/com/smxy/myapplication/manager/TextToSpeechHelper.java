package com.smxy.myapplication.manager;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import com.smxy.myapplication.utils.ErrorHandler;

import java.util.HashMap;
import java.util.Locale;

public class TextToSpeechHelper implements TextToSpeech.OnInitListener {
    private static final String TAG = "TextToSpeechHelper";
    private static TextToSpeechHelper instance;
    private TextToSpeech textToSpeech;
    private Context context;
    private boolean isInitialized = false;
    private OnSpeakListener speakListener;

    public interface OnSpeakListener {
        void onStart(String utteranceId);
        void onDone(String utteranceId);
        void onError(String utteranceId);
    }

    private TextToSpeechHelper(Context context) {
        if (context != null) {
            this.context = context.getApplicationContext();
            textToSpeech = new TextToSpeech(this.context, this);
        }
    }

    public static synchronized TextToSpeechHelper getInstance(Context context) {
        if (context == null) {
            return null;
        }
        if (instance == null) {
            instance = new TextToSpeechHelper(context);
        }
        return instance;
    }

    public void setOnSpeakListener(OnSpeakListener listener) {
        this.speakListener = listener;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS && textToSpeech != null) {
            int result = textToSpeech.setLanguage(Locale.CHINESE);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.ENGLISH);
                Log.w(TAG, "中文语言不支持，使用英文");
            }
            textToSpeech.setSpeechRate(0.9f);
            textToSpeech.setPitch(1.0f);
            isInitialized = true;
            Log.d(TAG, "TTS初始化成功");
        } else {
            Log.e(TAG, "TTS初始化失败");
        }
    }

    public void speak(String text) {
        speak(text, null);
    }

    public void speak(String text, String utteranceId) {
        if (!isInitialized || textToSpeech == null) {
            Log.w(TAG, "TTS未初始化，无法播报");
            return;
        }
        if (text == null || text.isEmpty()) {
            return;
        }

        if (speakListener != null) {
            textToSpeech.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    if (speakListener != null) speakListener.onStart(utteranceId);
                }

                @Override
                public void onDone(String utteranceId) {
                    if (speakListener != null) speakListener.onDone(utteranceId);
                }

                @Override
                public void onError(String utteranceId) {
                    if (speakListener != null) speakListener.onError(utteranceId);
                }
            });
        }

        HashMap<String, String> params = new HashMap<>();
        if (utteranceId != null) {
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
        }

        textToSpeech.speak(text, TextToSpeech.QUEUE_ADD, params);
    }

    public void speakRecipe(String recipeName, String ingredients, String steps, int cookTime) {
        StringBuilder sb = new StringBuilder();
        sb.append("菜谱：").append(ErrorHandler.getSafeString(recipeName, "未知菜谱")).append("。");
        sb.append("烹饪时间").append(cookTime).append("分钟。");
        sb.append("所需食材：").append(ErrorHandler.getSafeString(ingredients, "无")).append("。");
        sb.append("制作步骤：").append(ErrorHandler.getSafeString(steps, "无"));
        speak(sb.toString());
    }

    public void stop() {
        if (textToSpeech != null) {
            textToSpeech.stop();
        }
    }

    public void shutdown() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        instance = null;
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}