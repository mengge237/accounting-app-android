package com.smxy.myapplication.utils;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.smxy.myapplication.R;
import com.smxy.myapplication.fragment.ExportDialogFragment;
import com.smxy.myapplication.model.Record;
import com.smxy.myapplication.network.ApiService;
import com.smxy.myapplication.network.RecordsResponse;
import com.smxy.myapplication.network.RetrofitClient;

import java.io.File;
import java.util.List;

import retrofit2.Call;
import retrofit2.Response;

public class ExportHelper {

    private static final String TAG = "ExportHelper";

    private final FragmentActivity activity;
    private final ExportManager exportManager;
    private final String authToken;

    public ExportHelper(FragmentActivity activity) {
        this.activity = activity;
        this.exportManager = new ExportManager(activity);
        this.authToken = getAuthToken();
    }

    private String getAuthToken() {
        try {
            SharedPreferences authPrefs = activity.getSharedPreferences("auth", Context.MODE_PRIVATE);
            String token = authPrefs.getString("token", "");
            if (ErrorHandler.isStringEmpty(token)) {
                token = authPrefs.getString("access_token", "");
            }
            if (ErrorHandler.isStringEmpty(token)) {
                SharedPreferences userPrefs = activity.getSharedPreferences("user_data", Context.MODE_PRIVATE);
                token = userPrefs.getString("token", "");
            }
            return ErrorHandler.getSafeString(token);
        } catch (Exception e) {
            ErrorHandler.logError(TAG, "getAuthToken failed", e);
            return "";
        }
    }

    public void startExport() {
        try {
            if (!ErrorHandler.isActivityValid(activity)) {
                return;
            }
            if (ErrorHandler.isStringEmpty(authToken)) {
                ErrorHandler.showToast(activity, "请先登录");
                return;
            }
            fetchRecordsAndShowDialog();
        } catch (Exception e) {
            ErrorHandler.logError(TAG, "startExport failed", e);
            ErrorHandler.showToast(activity, "导出功能异常");
        }
    }

    private void fetchRecordsAndShowDialog() {
        if (!ErrorHandler.isActivityValid(activity)) return;

        ProgressDialog progressDialog = null;
        try {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setMessage("正在获取数据...");
            progressDialog.setCancelable(true);
            progressDialog.show();
        } catch (Exception e) {
            ErrorHandler.logError(TAG, "show progress dialog failed", e);
        }

        final ProgressDialog finalProgressDialog = progressDialog;
        try {
            ApiService apiService = RetrofitClient.getInstance().getApi();
            String authHeader = ErrorHandler.formatBearerToken(authToken);
            Call<RecordsResponse> call = apiService.getRecords(authHeader);

            call.enqueue(new ErrorHandler.SafeCallback<RecordsResponse>(activity, "获取数据失败") {
                @Override
                protected void onSuccess(RecordsResponse data) {
                    dismissDialog(finalProgressDialog);
                    try {
                        List<Record> records = data.getData();
                        if (ErrorHandler.isCollectionEmpty(records)) {
                            ErrorHandler.showToast(activity, "暂无数据可导出");
                        } else {
                            showExportRangeDialog(records);
                        }
                    } catch (Exception e) {
                        ErrorHandler.logError(TAG, "process records failed", e);
                        ErrorHandler.showToast(activity, "处理数据失败");
                    }
                }

                @Override
                protected void onError(String message) {
                    dismissDialog(finalProgressDialog);
                    super.onError(message);
                }
            });
        } catch (Exception e) {
            dismissDialog(finalProgressDialog);
            ErrorHandler.logError(TAG, "fetchRecordsAndShowDialog failed", e);
            ErrorHandler.showToast(activity, "获取数据失败");
        }
    }

    private void dismissDialog(ProgressDialog dialog) {
        try {
            if (dialog != null && dialog.isShowing()) {
                dialog.dismiss();
            }
        } catch (Exception e) {
            ErrorHandler.logWarning(TAG, "dismiss dialog failed", e);
        }
    }

    private void showExportRangeDialog(List<Record> records) {
        try {
            if (!ErrorHandler.isActivityValid(activity) || ErrorHandler.isCollectionEmpty(records)) {
                return;
            }

            ExportDialogFragment dialog = new ExportDialogFragment();
            dialog.setExportListener((rangeOption, customStartDate, customEndDate) ->
                    performExport(records, rangeOption, customStartDate, customEndDate));

            FragmentManager fm = activity.getSupportFragmentManager();
            if (fm != null && !activity.isFinishing()) {
                dialog.show(fm, "export_dialog");
            }
        } catch (Exception e) {
            ErrorHandler.logError(TAG, "showExportRangeDialog failed", e);
            ErrorHandler.showToast(activity, "显示导出对话框失败");
        }
    }

    private void performExport(List<Record> records, int rangeOption, Long customStartDate, Long customEndDate) {
        if (!ErrorHandler.isActivityValid(activity)) return;

        ProgressDialog progressDialog = null;
        try {
            progressDialog = new ProgressDialog(activity);
            progressDialog.setMessage("正在导出数据...");
            progressDialog.setCancelable(false);
            progressDialog.show();
        } catch (Exception e) {
            ErrorHandler.logError(TAG, "show progress dialog failed", e);
        }

        final ProgressDialog finalProgressDialog = progressDialog;
        try {
            if (exportManager == null || ErrorHandler.isCollectionEmpty(records)) {
                dismissDialog(finalProgressDialog);
                ErrorHandler.showToast(activity, "没有可导出的数据");
                return;
            }

            long[] timeRange = exportManager.getTimeRange(rangeOption, customStartDate, customEndDate);
            long startTime = timeRange[0];
            long endTime = timeRange[1];

            exportManager.exportRecordsInRange(records, startTime, endTime,
                    new ExportManager.ExportCallback() {
                        @Override
                        public void onSuccess(String fileName, Uri fileUri) {
                            dismissDialog(finalProgressDialog);
                            if (ErrorHandler.isActivityValid(activity)) {
                                showSuccessDialog(fileName, fileUri);
                            }
                        }

                        @Override
                        public void onError(String errorMessage) {
                            dismissDialog(finalProgressDialog);
                            ErrorHandler.showToast(activity, ErrorHandler.getSafeString(errorMessage, "导出失败"));
                        }
                    });
        } catch (Exception e) {
            dismissDialog(finalProgressDialog);
            ErrorHandler.logError(TAG, "performExport failed", e);
            ErrorHandler.showToast(activity, "导出失败: " + e.getMessage());
        }
    }

    private void showSuccessDialog(String fileName, Uri fileUri) {
        try {
            if (!ErrorHandler.isActivityValid(activity)) return;

            new AlertDialog.Builder(activity)
                    .setTitle("导出成功")
                    .setMessage("数据已导出到：\n" + getDisplayPath(fileUri, fileName))
                    .setPositiveButton("确定", null)
                    .setNegativeButton("查看文件", (dialog, which) -> openFileDirectory(fileUri))
                    .setCancelable(true)
                    .show();
        } catch (Exception e) {
            ErrorHandler.logError(TAG, "showSuccessDialog failed", e);
            ErrorHandler.showToast(activity, "导出成功，文件已保存在下载目录");
        }
    }

    private String getDisplayPath(Uri fileUri, String fileName) {
        try {
            if (fileUri != null && fileUri.getPath() != null) {
                return fileUri.getPath();
            }
        } catch (Exception e) {
            ErrorHandler.logWarning(TAG, "getDisplayPath failed", e);
        }
        return "下载目录/记账本导出/" + ErrorHandler.getSafeString(fileName, "data.csv");
    }

    private void openFileDirectory(Uri fileUri) {
        try {
            if (!ErrorHandler.isActivityValid(activity) || fileUri == null) {
                return;
            }

            String path = fileUri.getPath();
            if (path == null) {
                ErrorHandler.showToast(activity, "无法获取文件路径");
                return;
            }

            File file = new File(path);
            File parentDir = file.getParentFile();
            if (parentDir != null && parentDir.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.fromFile(parentDir), "resource/folder");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                activity.startActivity(intent);
            } else {
                openDownloadsFolder();
            }
        } catch (Exception e) {
            ErrorHandler.logError(TAG, "openFileDirectory failed", e);
            openDownloadsFolder();
        }
    }

    private void openDownloadsFolder() {
        try {
            if (!ErrorHandler.isActivityValid(activity)) return;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            activity.startActivity(Intent.createChooser(intent, "打开下载目录"));
        } catch (Exception e) {
            ErrorHandler.showToast(activity, "无法打开文件目录，文件已保存在下载目录");
        }
    }
}