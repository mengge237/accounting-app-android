package com.smxy.myapplication.manager;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.smxy.myapplication.R;
import com.smxy.myapplication.utils.ErrorHandler;

public class VoiceFloatManager {
    private static final String TAG = "VoiceFloatManager";
    private static final int RECORD_MAX_TIME = 30000;

    private WindowManager windowManager;
    private WindowManager.LayoutParams layoutParams;
    private View floatView;
    private FloatingActionButton fabVoice;
    private View rippleView;
    private Context context;
    private int screenWidth;
    private int screenHeight;

    private int lastX;
    private int lastY;
    private int paramX;
    private int paramY;
    private boolean isDragging = false;
    private boolean isRecording = false;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable recordTimeoutRunnable;

    private OnVoiceRecordListener listener;
    private VoiceRecognitionManager recognitionManager;

    public interface OnVoiceRecordListener {
        void onRecordStart();
        void onRecordEnd(String recognizedText);
        void onRecordError(String error);
    }

    public VoiceFloatManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context cannot be null");
        }
        this.context = context.getApplicationContext();
        this.windowManager = (WindowManager) this.context.getSystemService(Context.WINDOW_SERVICE);
        this.recognitionManager = VoiceRecognitionManager.getInstance(this.context);
        this.screenWidth = windowManager.getDefaultDisplay().getWidth();
        this.screenHeight = windowManager.getDefaultDisplay().getHeight();
    }

    public void setOnVoiceRecordListener(OnVoiceRecordListener listener) {
        this.listener = listener;
    }

    public void show() {
        if (floatView != null) return;
        if (context == null || windowManager == null) return;

        floatView = LayoutInflater.from(context).inflate(R.layout.layout_voice_float_button, null);
        fabVoice = floatView.findViewById(R.id.fab_voice_float);
        rippleView = floatView.findViewById(R.id.ripple_view);

        int type;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            type = WindowManager.LayoutParams.TYPE_PHONE;
        }

        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = screenWidth - 150;
        layoutParams.y = 300;

        try {
            windowManager.addView(floatView, layoutParams);
            setupTouchListener();
            setupClickListener();
        } catch (Exception e) {
            Log.e(TAG, "显示悬浮窗失败", e);
        }
    }

    private void setupTouchListener() {
        if (floatView == null) return;

        floatView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastX = (int) event.getRawX();
                    lastY = (int) event.getRawY();
                    paramX = layoutParams.x;
                    paramY = layoutParams.y;
                    isDragging = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    int dx = (int) event.getRawX() - lastX;
                    int dy = (int) event.getRawY() - lastY;

                    if (Math.abs(dx) > 3 || Math.abs(dy) > 3) {
                        isDragging = true;
                        int newX = paramX + dx;
                        int newY = paramY + dy;

                        newX = Math.max(0, Math.min(newX, screenWidth - floatView.getWidth()));
                        newY = Math.max(0, Math.min(newY, screenHeight - floatView.getHeight()));

                        layoutParams.x = newX;
                        layoutParams.y = newY;
                        windowManager.updateViewLayout(floatView, layoutParams);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                    if (!isDragging) {
                        v.performClick();
                    }
                    return true;
            }
            return false;
        });
    }

    private void setupClickListener() {
        if (fabVoice == null) return;

        fabVoice.setOnClickListener(v -> {
            if (!isRecording) {
                startRecording();
            } else {
                stopRecording();
            }
        });
    }

    private void startRecording() {
        if (isRecording) return;
        if (context == null) return;

        isRecording = true;

        if (fabVoice != null) {
            fabVoice.setImageResource(R.drawable.ic_stop);
            fabVoice.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFF44336));
        }
        if (rippleView != null) {
            rippleView.setVisibility(View.VISIBLE);
            startRippleAnimation();
        }

        recordTimeoutRunnable = () -> {
            if (isRecording) {
                stopRecording();
                Toast.makeText(context, "录音时间过长，已自动停止", Toast.LENGTH_SHORT).show();
            }
        };
        handler.postDelayed(recordTimeoutRunnable, RECORD_MAX_TIME);

        if (listener != null) {
            listener.onRecordStart();
        }

        if (recognitionManager != null) {
            recognitionManager.startListening(new VoiceRecognitionManager.RecognitionCallback() {
                @Override
                public void onPartialResult(String text) {
                    Log.d(TAG, "临时识别: " + text);
                }

                @Override
                public void onFinalResult(String text) {
                    Log.d(TAG, "最终识别结果: " + text);
                    if (listener != null && text != null && !text.isEmpty()) {
                        listener.onRecordEnd(text);
                    }
                    if (isRecording) {
                        stopRecording();
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "识别错误: " + error);
                    if (listener != null) {
                        listener.onRecordError(error);
                    }
                    if (isRecording) {
                        stopRecording();
                    }
                }
            });
        }

        Toast.makeText(context, "开始录音，点击悬浮球结束", Toast.LENGTH_SHORT).show();
    }

    private void startRippleAnimation() {
        if (rippleView == null) return;

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(rippleView, "scaleX", 1f, 1.5f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(rippleView, "scaleY", 1f, 1.5f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(rippleView, "alpha", 1f, 0f);

        scaleX.setDuration(800);
        scaleY.setDuration(800);
        alpha.setDuration(800);
        scaleX.setRepeatCount(ValueAnimator.INFINITE);
        scaleY.setRepeatCount(ValueAnimator.INFINITE);
        alpha.setRepeatCount(ValueAnimator.INFINITE);

        scaleX.start();
        scaleY.start();
        alpha.start();

        rippleView.setTag(new Object[]{scaleX, scaleY, alpha});
    }

    private void stopRippleAnimation() {
        if (rippleView == null) return;

        Object[] animators = (Object[]) rippleView.getTag();
        if (animators != null) {
            for (Object anim : animators) {
                if (anim instanceof ObjectAnimator) {
                    ((ObjectAnimator) anim).cancel();
                }
            }
        }
        rippleView.clearAnimation();
        rippleView.setScaleX(1f);
        rippleView.setScaleY(1f);
        rippleView.setAlpha(1f);
    }

    private void stopRecording() {
        if (!isRecording) return;

        isRecording = false;

        stopRippleAnimation();
        if (rippleView != null) {
            rippleView.setVisibility(View.GONE);
        }

        if (fabVoice != null) {
            fabVoice.setImageResource(R.drawable.ic_microphone);
            fabVoice.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50));
        }

        if (recordTimeoutRunnable != null) {
            handler.removeCallbacks(recordTimeoutRunnable);
        }

        if (recognitionManager != null) {
            recognitionManager.stopListening();
        }

        if (context != null) {
            Toast.makeText(context, "录音结束", Toast.LENGTH_SHORT).show();
        }
    }

    public void hide() {
        if (floatView != null && windowManager != null) {
            try {
                if (isRecording) {
                    stopRecording();
                }
                windowManager.removeView(floatView);
            } catch (Exception e) {
                Log.e(TAG, "移除悬浮窗失败", e);
            }
            floatView = null;
        }
    }
}