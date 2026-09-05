package com.smxy.myapplication.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.smxy.myapplication.R;
import com.smxy.myapplication.network.ApiResponse;
import com.smxy.myapplication.network.ApiService;
import com.smxy.myapplication.network.RetrofitClient;
import com.smxy.myapplication.utils.ErrorHandler;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    private ApiService apiService;
    private EditText etUsername;
    private EditText etNewPassword;
    private EditText etConfirmPassword;
    private Button btnToggleNewPassword;
    private Button btnToggleConfirmPassword;
    private TextView tvBackToLogin;
    private Button btnResetPassword;

    private boolean isNewPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        apiService = RetrofitClient.getApiService();

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("重置密码");
        }

        initViews();
        setupPasswordToggles();
        setupListeners();
    }

    private void initViews() {
        etUsername = findViewById(R.id.et_reset_username);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_new_password);
        btnToggleNewPassword = findViewById(R.id.btn_toggle_new_password);
        btnToggleConfirmPassword = findViewById(R.id.btn_toggle_confirm_password);
        tvBackToLogin = findViewById(R.id.tv_back_to_login);
        btnResetPassword = findViewById(R.id.btn_reset_password);
    }

    private void setupPasswordToggles() {
        if (btnToggleNewPassword != null && etNewPassword != null) {
            btnToggleNewPassword.setOnClickListener(v -> {
                if (isNewPasswordVisible) {
                    etNewPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    btnToggleNewPassword.setText("👁");
                    isNewPasswordVisible = false;
                } else {
                    etNewPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    btnToggleNewPassword.setText("🙈");
                    isNewPasswordVisible = true;
                }
                etNewPassword.setSelection(etNewPassword.getText().length());
            });
        }

        if (btnToggleConfirmPassword != null && etConfirmPassword != null) {
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
    }

    private void setupListeners() {
        if (btnResetPassword != null) {
            btnResetPassword.setOnClickListener(v -> performResetPassword());
        }

        if (tvBackToLogin != null) {
            tvBackToLogin.setOnClickListener(v -> {
                setResult(RESULT_CANCELED);
                finish();
            });
        }
    }

    private void performResetPassword() {
        if (etUsername == null || etNewPassword == null || etConfirmPassword == null) {
            ErrorHandler.showToast(this, "输入框初始化失败");
            return;
        }

        String username = etUsername.getText().toString().trim();
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (username.isEmpty()) {
            etUsername.setError("请输入用户名");
            ErrorHandler.showToast(this, "请输入用户名");
            return;
        }

        if (newPassword.isEmpty()) {
            etNewPassword.setError("请输入新密码");
            ErrorHandler.showToast(this, "请输入新密码");
            return;
        }

        if (newPassword.length() < 6) {
            etNewPassword.setError("密码长度至少6位");
            ErrorHandler.showToast(this, "密码长度至少6位");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            etConfirmPassword.setError("两次输入的密码不一致");
            ErrorHandler.showToast(this, "两次输入的密码不一致");
            return;
        }

        ErrorHandler.showToast(this, "正在重置密码...");

        Map<String, String> params = new HashMap<>();
        params.put("username", username);
        params.put("new_password", newPassword);

        apiService.resetPassword(params).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        ErrorHandler.showToast(ResetPasswordActivity.this, "密码重置成功");

                        Intent intent = new Intent();
                        intent.putExtra("reset_username", username);
                        intent.putExtra("success", true);
                        setResult(RESULT_OK, intent);
                        finish();
                    } else {
                        String msg = response.body() != null ? response.body().getMessage() : "密码重置失败";
                        ErrorHandler.showToast(ResetPasswordActivity.this, msg);
                    }
                });
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                runOnUiThread(() -> ErrorHandler.showToast(ResetPasswordActivity.this, "网络错误: " + t.getMessage()));
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}