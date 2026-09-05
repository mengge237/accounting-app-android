package com.smxy.myapplication.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.smxy.myapplication.R;
import com.smxy.myapplication.network.ApiService;
import com.smxy.myapplication.network.LoginResponse;
import com.smxy.myapplication.network.RetrofitClient;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREFS_NAME = "user_data";
    private static final String KEY_LAST_USERNAME = "last_username";
    private static final String KEY_LAST_PASSWORD = "last_password";
    private static final String KEY_REMEMBER_PASSWORD = "remember_password";
    private static final String KEY_TOKEN = "token";

    private EditText etUsername;
    private EditText etPassword;
    private CheckBox cbRememberPassword;
    private Button btnTogglePassword;
    private SharedPreferences sharedPreferences;
    private ApiService apiService;
    private boolean isPasswordVisible = false;
    private Call<LoginResponse> currentLoginCall;
    private boolean isActivityDestroyed = false;

    private final ActivityResultLauncher<Intent> registerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::handleRegisterResult
    );

    private final ActivityResultLauncher<Intent> resetPasswordLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            this::handleResetPasswordResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate called");

        try {
            EdgeToEdge.enable(this);
            setContentView(R.layout.activity_login);

            if (isFinishing() || isDestroyed()) {
                Log.w(TAG, "Activity is finishing or destroyed, skipping initialization");
                return;
            }

            initRetrofitClient();
            sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.activity_login_root), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });

            initViews();
            setupPasswordToggle();
            autoFillLastUser();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: ", e);
            ErrorHandler.showToast(this, "初始化失败: " + e.getMessage());
            finish();
        }
    }

    private void initRetrofitClient() {
        try {
            OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .retryOnConnectionFailure(true)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://10.0.2.2:8080/")
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            apiService = retrofit.create(ApiService.class);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Retrofit: ", e);
            apiService = RetrofitClient.getApiService();
        }
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        Button btnLogin = findViewById(R.id.btn_login);
        TextView tvRegisterLink = findViewById(R.id.tv_register_link);
        TextView tvResetPasswordLink = findViewById(R.id.tv_reset_password_link);
        cbRememberPassword = findViewById(R.id.cb_remember_password);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);

        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> performLogin());
        } else {
            Log.e(TAG, "Login button not found");
            ErrorHandler.showToast(this, "登录按钮未找到");
        }

        if (tvRegisterLink != null) {
            tvRegisterLink.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                registerLauncher.launch(intent);
            });
        }

        if (tvResetPasswordLink != null) {
            tvResetPasswordLink.setOnClickListener(v -> {
                Intent intent = new Intent(LoginActivity.this, ResetPasswordActivity.class);
                resetPasswordLauncher.launch(intent);
            });
        }
    }

    private void setupPasswordToggle() {
        if (btnTogglePassword == null || etPassword == null) {
            Log.w(TAG, "Password toggle button or password field is null");
            return;
        }

        btnTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnTogglePassword.setText("👁");
                isPasswordVisible = false;
            } else {
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnTogglePassword.setText("🙈");
                isPasswordVisible = true;
            }
            etPassword.setSelection(etPassword.getText().length());
        });
    }

    private void autoFillLastUser() {
        if (etUsername == null || etPassword == null || cbRememberPassword == null) {
            return;
        }

        boolean isRemembered = sharedPreferences.getBoolean(KEY_REMEMBER_PASSWORD, false);
        String lastUsername = sharedPreferences.getString(KEY_LAST_USERNAME, "");

        if (isRemembered && !lastUsername.isEmpty()) {
            String lastPassword = sharedPreferences.getString(KEY_LAST_PASSWORD, "");
            etUsername.setText(lastUsername);
            cbRememberPassword.setChecked(true);
            if (!lastPassword.isEmpty()) {
                etPassword.setText(lastPassword);
            }
            etPassword.requestFocus();
        } else if (!lastUsername.isEmpty()) {
            etUsername.setText(lastUsername);
            etPassword.requestFocus();
            cbRememberPassword.setChecked(false);
        }
    }

    private void handleRegisterResult(ActivityResult result) {
        if (isActivityDestroyed) return;

        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            String username = result.getData().getStringExtra("registered_username");
            if (username != null && etUsername != null) {
                String successMsg = "注册成功！欢迎 " + username;
                ErrorHandler.showToast(this, successMsg);
                etUsername.setText(username);
                if (etPassword != null) {
                    etPassword.setText("");
                }
                sharedPreferences.edit().putString(KEY_LAST_USERNAME, username).apply();
            }
        }
    }

    private void handleResetPasswordResult(ActivityResult result) {
        if (isActivityDestroyed) return;

        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
            String username = result.getData().getStringExtra("reset_username");
            boolean success = result.getData().getBooleanExtra("success", false);

            if (success && username != null && etUsername != null && etPassword != null) {
                ErrorHandler.showToast(this, "密码重置成功，请使用新密码登录");
                etUsername.setText(username);
                etPassword.setText("");
                etPassword.requestFocus();

                sharedPreferences.edit()
                        .remove(KEY_LAST_PASSWORD)
                        .putBoolean(KEY_REMEMBER_PASSWORD, false)
                        .apply();

                if (cbRememberPassword != null) {
                    cbRememberPassword.setChecked(false);
                }
            } else if (!success) {
                ErrorHandler.showToast(this, "密码重置失败，请重试");
            }
        }
    }

    private void performLogin() {
        if (currentLoginCall != null && !currentLoginCall.isCanceled()) {
            currentLoginCall.cancel();
        }

        if (etUsername == null || etPassword == null) {
            ErrorHandler.showToast(this, "输入框初始化失败");
            return;
        }

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (username.isEmpty()) {
            etUsername.setError("请输入用户名");
            ErrorHandler.showToast(this, "请输入用户名");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("请输入密码");
            ErrorHandler.showToast(this, "请输入密码");
            return;
        }

        Button btnLogin = findViewById(R.id.btn_login);
        if (btnLogin != null) {
            btnLogin.setEnabled(false);
        }

        ErrorHandler.showToast(this, "正在登录...");

        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);

        if (apiService == null) {
            ErrorHandler.showToast(this, "网络服务初始化失败");
            if (btnLogin != null) btnLogin.setEnabled(true);
            return;
        }

        currentLoginCall = apiService.login(params);
        currentLoginCall.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(@NonNull Call<LoginResponse> call, @NonNull Response<LoginResponse> response) {
                if (isActivityDestroyed || isFinishing() || isDestroyed()) {
                    Log.d(TAG, "Activity is destroyed, ignoring response");
                    return;
                }

                runOnUiThread(() -> {
                    Button loginBtn = findViewById(R.id.btn_login);
                    if (loginBtn != null) loginBtn.setEnabled(true);

                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        LoginResponse loginResponse = response.body();
                        String token = loginResponse.getToken();
                        String loggedUsername = loginResponse.getUser() != null ?
                                loginResponse.getUser().getUsername() : username;

                        Log.d(TAG, "Login success, token: " + (token != null ? "received" : "null"));

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString(KEY_LAST_USERNAME, loggedUsername);

                        if (cbRememberPassword != null && cbRememberPassword.isChecked()) {
                            editor.putString(KEY_LAST_PASSWORD, password);
                            editor.putBoolean(KEY_REMEMBER_PASSWORD, true);
                        } else {
                            editor.remove(KEY_LAST_PASSWORD);
                            editor.putBoolean(KEY_REMEMBER_PASSWORD, false);
                        }
                        editor.putString(KEY_TOKEN, token);
                        editor.apply();

                        SharedPreferences authPrefs = getSharedPreferences("auth", MODE_PRIVATE);
                        authPrefs.edit()
                                .putString("token", token)
                                .putString("username", loggedUsername)
                                .apply();

                        ErrorHandler.showToast(LoginActivity.this, "登录成功");

                        Intent intent = new Intent(LoginActivity.this, MainContainerActivity.class);
                        intent.putExtra("username", loggedUsername);
                        intent.putExtra("token", token);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        String msg = response.body() != null ? response.body().getMessage() : "登录失败";
                        ErrorHandler.showToast(LoginActivity.this, msg);
                        if (etPassword != null) {
                            etPassword.setText("");
                            etPassword.requestFocus();
                        }
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<LoginResponse> call, @NonNull Throwable t) {
                if (isActivityDestroyed || isFinishing() || isDestroyed()) {
                    Log.d(TAG, "Activity is destroyed, ignoring failure");
                    return;
                }

                runOnUiThread(() -> {
                    Button loginBtn = findViewById(R.id.btn_login);
                    if (loginBtn != null) loginBtn.setEnabled(true);

                    if (call.isCanceled()) {
                        Log.d(TAG, "Login request was cancelled");
                        return;
                    }

                    String errorMsg = t.getMessage();
                    if (errorMsg != null && errorMsg.contains("Failed to connect")) {
                        ErrorHandler.showToast(LoginActivity.this, "无法连接到服务器，请检查网络");
                    } else if (errorMsg != null && errorMsg.contains("timeout")) {
                        ErrorHandler.showToast(LoginActivity.this, "连接超时，请稍后重试");
                    } else {
                        ErrorHandler.showToast(LoginActivity.this, "网络错误: " + errorMsg);
                    }

                    Log.e(TAG, "Login failure: ", t);
                });
            }
        });
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("password_visible", isPasswordVisible);
        if (etUsername != null) {
            outState.putString("username", etUsername.getText().toString());
        }
        if (etPassword != null) {
            outState.putString("password", etPassword.getText().toString());
        }
        if (cbRememberPassword != null) {
            outState.putBoolean("remember_password", cbRememberPassword.isChecked());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isActivityDestroyed = true;

        if (currentLoginCall != null && !currentLoginCall.isCanceled()) {
            currentLoginCall.cancel();
            currentLoginCall = null;
        }

        Log.d(TAG, "onDestroy called, cleaned up resources");
    }
}