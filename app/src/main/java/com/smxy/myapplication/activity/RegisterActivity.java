package com.smxy.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.smxy.myapplication.R;
import com.smxy.myapplication.network.ApiResponse;
import com.smxy.myapplication.network.ApiService;
import com.smxy.myapplication.network.CheckUsernameResponse;
import com.smxy.myapplication.network.RetrofitClient;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private ApiService apiService;
    private EditText etUsername;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private EditText etEmail;
    private Button btnTogglePassword;
    private Button btnToggleConfirmPassword;
    private TextView tvBackToLogin;
    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        apiService = RetrofitClient.getApiService();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        initViews();
        setupPasswordToggle();
        setupConfirmPasswordToggle();
        setupBackToLogin();

        Button btnRegister = findViewById(R.id.btn_register);
        if (btnRegister != null) {
            btnRegister.setOnClickListener(v -> performRegistration());
        }
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_register_username);
        etPassword = findViewById(R.id.et_register_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etEmail = findViewById(R.id.et_email);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        btnToggleConfirmPassword = findViewById(R.id.btn_toggle_confirm_password);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);
    }

    private void setupPasswordToggle() {
        if (btnTogglePassword == null || etPassword == null) return;

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

    private void setupConfirmPasswordToggle() {
        if (btnToggleConfirmPassword == null || etConfirmPassword == null) return;

        btnToggleConfirmPassword.setOnClickListener(v -> {
            if (isConfirmPasswordVisible) {
                etConfirmPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                btnToggleConfirmPassword.setText("👁");
                isConfirmPasswordVisible = false;
            } else {
                etConfirmPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                btnToggleConfirmPassword.setText("🙈");
                isConfirmPasswordVisible = true;
            }
            etConfirmPassword.setSelection(etConfirmPassword.getText().length());
        });
    }

    private void setupBackToLogin() {
        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(v -> finish());
        }
    }

    private void performRegistration() {
        if (etUsername == null || etPassword == null || etConfirmPassword == null) {
            ErrorHandler.showToast(this, "输入框初始化失败");
            return;
        }

        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        String email = etEmail != null ? etEmail.getText().toString().trim() : "";

        if (username.isEmpty()) {
            etUsername.setError("请输入用户名");
            ErrorHandler.showToast(this, "请输入用户名");
            return;
        }

        if (username.length() < 3) {
            etUsername.setError("用户名至少3个字符");
            ErrorHandler.showToast(this, "用户名至少3个字符");
            return;
        }

        if (password.isEmpty()) {
            etPassword.setError("请输入密码");
            ErrorHandler.showToast(this, "请输入密码");
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("密码至少6位");
            ErrorHandler.showToast(this, "密码至少6位");
            return;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("两次输入的密码不一致");
            ErrorHandler.showToast(this, "两次输入的密码不一致");
            return;
        }

        if (!email.isEmpty() && !isValidEmail(email)) {
            if (etEmail != null) etEmail.setError("邮箱格式不正确");
            ErrorHandler.showToast(this, "邮箱格式不正确");
            return;
        }

        ErrorHandler.showToast(this, "正在检查用户名...");

        apiService.checkUsername(username).enqueue(new Callback<CheckUsernameResponse>() {
            @Override
            public void onResponse(@NonNull Call<CheckUsernameResponse> call, @NonNull Response<CheckUsernameResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isExists()) {
                    runOnUiThread(() -> {
                        if (etUsername != null) etUsername.setError("用户名已存在");
                        ErrorHandler.showToast(RegisterActivity.this, "用户名已存在");
                    });
                } else {
                    registerUser(username, password, email);
                }
            }

            @Override
            public void onFailure(@NonNull Call<CheckUsernameResponse> call, @NonNull Throwable t) {
                runOnUiThread(() -> ErrorHandler.showToast(RegisterActivity.this, "网络错误: " + t.getMessage()));
            }
        });
    }

    private void registerUser(String username, String password, String email) {
        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("password", password);
        if (!email.isEmpty()) {
            params.put("email", email);
        }

        apiService.register(params).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        ErrorHandler.showToast(RegisterActivity.this, "注册成功");

                        Intent intent = new Intent();
                        intent.putExtra("registered_username", username);
                        setResult(RESULT_OK, intent);
                        finish();
                    } else {
                        String msg = response.body() != null ? response.body().getMessage() : "注册失败";
                        ErrorHandler.showToast(RegisterActivity.this, msg);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                runOnUiThread(() -> ErrorHandler.showToast(RegisterActivity.this, "网络错误: " + t.getMessage()));
            }
        });
    }

    private boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
        return email.matches(emailPattern);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}