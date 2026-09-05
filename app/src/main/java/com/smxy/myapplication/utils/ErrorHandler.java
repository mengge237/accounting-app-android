package com.smxy.myapplication.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

import com.smxy.myapplication.R;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ErrorHandler {

    private static final String TAG = "ErrorHandler";

    public static boolean isFragmentValid(@Nullable Fragment fragment) {
        if (fragment == null) return false;
        try {
            return fragment.isAdded() && !fragment.isDetached() && !fragment.isRemoving()
                    && fragment.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.CREATED);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isActivityValid(@Nullable Activity activity) {
        if (activity == null) return false;
        try {
            return !activity.isFinishing() && !activity.isDestroyed();
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isContextValid(@Nullable Context context) {
        return context != null;
    }

    public static boolean isActivityContextValid(@Nullable Context context) {
        if (context == null) return false;
        if (context instanceof Activity) {
            return isActivityValid((Activity) context);
        }
        return true;
    }

    public static void showToast(@Nullable Context context, String message) {
        if (context == null || message == null) return;
        Context safeContext = context.getApplicationContext();
        try {
            Toast.makeText(safeContext, message, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.w(TAG, "showToast failed", e);
        }
    }

    public static void showToast(@Nullable Context context, int messageResId) {
        if (context == null) return;
        Context safeContext = context.getApplicationContext();
        try {
            Toast.makeText(safeContext, messageResId, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.w(TAG, "showToast failed", e);
        }
    }

    public static void showLongToast(@Nullable Context context, String message) {
        if (context == null || message == null) return;
        Context safeContext = context.getApplicationContext();
        try {
            Toast.makeText(safeContext, message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.w(TAG, "showLongToast failed", e);
        }
    }

    public static void logError(String tag, String message, @Nullable Throwable throwable) {
        try {
            if (throwable != null) {
                Log.e(tag, message, throwable);
            } else {
                Log.e(tag, message);
            }
        } catch (Exception e) {
            System.err.println(tag + ": " + message);
        }
    }

    public static void logWarning(String tag, String message, @Nullable Throwable throwable) {
        try {
            if (throwable != null) {
                Log.w(tag, message, throwable);
            } else {
                Log.w(tag, message);
            }
        } catch (Exception e) {
            System.err.println(tag + ": " + message);
        }
    }

    public static void logInfo(String tag, String message) {
        try {
            Log.i(tag, message);
        } catch (Exception e) {
            System.out.println(tag + ": " + message);
        }
    }

    public static void safeRun(@Nullable Context context, String errorMessage, Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            logError(TAG, errorMessage, e);
            showToast(context, errorMessage);
        }
    }

    public static void safeRunSilent(Runnable action) {
        try {
            action.run();
        } catch (Exception e) {
            logWarning(TAG, "silent operation failed", e);
        }
    }

    public static <T> T safeGet(SafeGetter<T> getter, T defaultValue) {
        try {
            T result = getter.get();
            return result != null ? result : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public interface SafeGetter<T> {
        T get() throws Exception;
    }

    public static boolean isStringEmpty(@Nullable String str) {
        return str == null || str.trim().isEmpty();
    }

    public static boolean isCollectionEmpty(@Nullable Collection<?> collection) {
        return collection == null || collection.isEmpty();
    }

    public static boolean isMapEmpty(@Nullable Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    public static String getSafeString(@Nullable String str, String defaultValue) {
        return (str != null && !str.trim().isEmpty()) ? str : defaultValue;
    }

    public static String getSafeString(@Nullable String str) {
        return getSafeString(str, "");
    }

    public static int getSafeInt(@Nullable Integer value, int defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static long getSafeLong(@Nullable Long value, long defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static double getSafeDouble(@Nullable Double value, double defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static boolean getSafeBoolean(@Nullable Boolean value, boolean defaultValue) {
        return value != null ? value : defaultValue;
    }

    public static boolean isTokenValid(@Nullable String token) {
        return token != null && !token.trim().isEmpty() && !"Bearer ".equals(token);
    }

    public static String formatBearerToken(@Nullable String token) {
        if (token == null || token.trim().isEmpty()) return "";
        if (token.startsWith("Bearer ")) return token;
        return "Bearer " + token;
    }

    public static void closeQuietly(@Nullable Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                logWarning(TAG, "Failed to close resource", e);
            }
        }
    }

    public static void closeQuietly(@Nullable AutoCloseable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                logWarning(TAG, "Failed to close resource", e);
            }
        }
    }

    public static String getExceptionMessage(@Nullable Throwable throwable, String defaultMessage) {
        if (throwable == null) return defaultMessage;
        String message = throwable.getMessage();
        return message != null && !message.isEmpty() ? message : defaultMessage;
    }

    public static abstract class SafeCallback<T> implements Callback<T> {
        private final Fragment fragment;
        private final Activity activity;
        private final String errorPrefix;

        public SafeCallback(Fragment fragment) {
            this(fragment, null);
        }

        public SafeCallback(Fragment fragment, String errorPrefix) {
            this.fragment = fragment;
            this.activity = null;
            this.errorPrefix = errorPrefix;
        }

        public SafeCallback(Activity activity, String errorPrefix) {
            this.fragment = null;
            this.activity = activity;
            this.errorPrefix = errorPrefix;
        }

        @Override
        public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
            if (!isValid()) {
                return;
            }

            try {
                if (response.isSuccessful() && response.body() != null) {
                    onSuccess(response.body());
                } else {
                    String errorMsg = getErrorMessage(response);
                    onError(errorMsg);
                }
            } catch (Exception e) {
                logError(TAG, "onResponse processing failed", e);
                onError("处理响应失败");
            }
        }

        @Override
        public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
            if (!isValid()) {
                return;
            }
            String errorMsg = getNetworkErrorMessage(t);
            onError(errorMsg);
        }

        private boolean isValid() {
            if (fragment != null) {
                return isFragmentValid(fragment);
            }
            if (activity != null) {
                return isActivityValid(activity);
            }
            return true;
        }

        protected abstract void onSuccess(T data);

        protected void onError(String message) {
            Context context = null;
            if (fragment != null) {
                context = fragment.getContext();
            } else if (activity != null) {
                context = activity;
            }
            showToast(context, message);
        }

        private String getErrorMessage(Response<T> response) {
            String prefix = errorPrefix != null ? errorPrefix : "操作失败";
            try {
                int code = response.code();
                if (code == 401) {
                    return "登录已过期，请重新登录";
                } else if (code == 403) {
                    return "无权限执行此操作";
                } else if (code == 404) {
                    return "请求的资源不存在";
                } else if (code >= 500) {
                    return "服务器错误，请稍后重试";
                }
                return prefix + " (错误码: " + code + ")";
            } catch (Exception e) {
                return prefix + "，请稍后重试";
            }
        }

        private String getNetworkErrorMessage(Throwable t) {
            String prefix = errorPrefix != null ? errorPrefix : "网络错误";
            if (t != null && t.getMessage() != null) {
                return prefix + ": " + t.getMessage();
            }
            return prefix + "，请检查网络连接";
        }
    }
}