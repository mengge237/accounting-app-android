package com.smxy.myapplication.utils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.smxy.myapplication.model.Record;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExportManager {
    private static final String TAG = "ExportManager";
    private static final String CSV_HEADER = "ID,类型,金额,分类,备注,记录日期,创建时间\n";
    private static final String EXPORT_DIR_NAME = "记账本导出";

    private final Context context;
    private final SimpleDateFormat fileDateFormat;

    public ExportManager(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context must not be null");
        }
        this.context = context.getApplicationContext();
        this.fileDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA);
    }

    public void exportRecords(List<Record> records, ExportCallback callback) {
        try {
            if (ErrorHandler.isCollectionEmpty(records)) {
                if (callback != null) {
                    callback.onError("没有可导出的数据");
                }
                return;
            }

            String fileName = generateFileName();
            String csvContent = buildCSVContent(records);

            if (TextUtils.isEmpty(csvContent)) {
                if (callback != null) {
                    callback.onError("导出内容为空");
                }
                return;
            }

            Uri fileUri = saveCSVFile(fileName, csvContent);

            if (fileUri != null && callback != null) {
                callback.onSuccess(fileName, fileUri);
            } else if (callback != null) {
                callback.onError("导出失败：无法保存文件");
            }

        } catch (Exception e) {
            ErrorHandler.logError(TAG, "导出失败", e);
            if (callback != null) {
                callback.onError("导出失败：" + ErrorHandler.getExceptionMessage(e, "未知错误"));
            }
        }
    }

    public void exportRecordsInRange(List<Record> allRecords,
                                     long startTime,
                                     long endTime,
                                     ExportCallback callback) {
        try {
            if (ErrorHandler.isCollectionEmpty(allRecords)) {
                if (callback != null) {
                    callback.onError("没有可导出的数据");
                }
                return;
            }

            if (startTime > endTime) {
                long temp = startTime;
                startTime = endTime;
                endTime = temp;
            }

            List<Record> filteredRecords = new ArrayList<>();
            for (Record record : allRecords) {
                if (record == null) continue;
                try {
                    long timestamp = record.getTimestamp();
                    if (timestamp >= startTime && timestamp <= endTime) {
                        filteredRecords.add(record);
                    }
                } catch (Exception e) {
                    ErrorHandler.logWarning(TAG, "skip invalid record", e);
                }
            }

            if (filteredRecords.isEmpty()) {
                if (callback != null) {
                    callback.onError("所选日期范围内没有数据");
                }
                return;
            }

            exportRecords(filteredRecords, callback);

        } catch (Exception e) {
            ErrorHandler.logError(TAG, "exportRecordsInRange failed", e);
            if (callback != null) {
                callback.onError("导出失败：" + ErrorHandler.getExceptionMessage(e, "未知错误"));
            }
        }
    }

    private String generateFileName() {
        try {
            return String.format(Locale.CHINA, "记账数据_%s.csv",
                    fileDateFormat.format(new Date()));
        } catch (Exception e) {
            return "记账数据_" + System.currentTimeMillis() + ".csv";
        }
    }

    private String buildCSVContent(List<Record> records) {
        try {
            StringBuilder sb = new StringBuilder();

            sb.append("\uFEFF");
            sb.append(CSV_HEADER);

            for (Record record : records) {
                if (record == null) continue;

                try {
                    sb.append(escapeCSV(safeStringValue(record.getId()))).append(",");
                    sb.append(escapeCSV(safeStringValue(record.getCategoryName()))).append(",");
                    sb.append(safeAmountValue(record.getAmount())).append(",");
                    sb.append(escapeCSV(safeStringValue(record.getTypeName()))).append(",");
                    sb.append(escapeCSV(safeStringValue(record.getNote()))).append(",");
                    sb.append(escapeCSV(safeStringValue(record.getDisplayDate()))).append(",");
                    sb.append(escapeCSV(safeStringValue(record.getCreatedAt()))).append("\n");
                } catch (Exception e) {
                    ErrorHandler.logWarning(TAG, "skip invalid record line", e);
                }
            }

            return sb.toString();
        } catch (Exception e) {
            ErrorHandler.logError(TAG, "buildCSVContent failed", e);
            return null;
        }
    }

    private String safeStringValue(Object value) {
        if (value == null) return "";
        try {
            return String.valueOf(value);
        } catch (Exception e) {
            return "";
        }
    }

    private String safeAmountValue(double amount) {
        try {
            return String.valueOf(amount);
        } catch (Exception e) {
            return "0";
        }
    }

    private String escapeCSV(String value) {
        try {
            if (value == null || value.isEmpty()) {
                return "\"\"";
            }

            if (value.contains(",") || value.contains("\n") || value.contains("\r") || value.contains("\"")) {
                value = value.replace("\"", "\"\"");
                return "\"" + value + "\"";
            }

            return value;
        } catch (Exception e) {
            return "\"\"";
        }
    }

    private Uri saveCSVFile(String fileName, String content) {
        try {
            if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(content)) {
                return null;
            }

            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/" + EXPORT_DIR_NAME);
            } else {
                values.put(MediaStore.MediaColumns.DATA,
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                                + "/" + EXPORT_DIR_NAME + "/" + fileName);
            }

            ContentResolver resolver = context.getContentResolver();
            if (resolver == null) {
                return saveCSVFileLegacy(fileName, content);
            }

            Uri uri = null;
            try {
                uri = resolver.insert(MediaStore.Files.getContentUri("external"), values);
            } catch (Exception e) {
                ErrorHandler.logWarning(TAG, "MediaStore insert failed", e);
            }

            if (uri != null) {
                FileOutputStream fos = null;
                OutputStreamWriter writer = null;
                try {
                    fos = (FileOutputStream) resolver.openOutputStream(uri);
                    if (fos == null) {
                        return saveCSVFileLegacy(fileName, content);
                    }
                    writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
                    writer.write(content);
                    writer.flush();
                    return uri;
                } finally {
                    ErrorHandler.closeQuietly(writer);
                    ErrorHandler.closeQuietly(fos);
                }
            }

            return saveCSVFileLegacy(fileName, content);

        } catch (Exception e) {
            ErrorHandler.logError(TAG, "保存文件失败", e);
            return saveCSVFileLegacy(fileName, content);
        }
    }

    private Uri saveCSVFileLegacy(String fileName, String content) {
        FileOutputStream fos = null;
        OutputStreamWriter writer = null;
        try {
            if (TextUtils.isEmpty(fileName) || TextUtils.isEmpty(content)) {
                return null;
            }

            File downloadDir = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), EXPORT_DIR_NAME);

            if (!downloadDir.exists()) {
                boolean created = downloadDir.mkdirs();
                if (!created) {
                    ErrorHandler.logWarning(TAG, "创建目录失败: " + downloadDir.getAbsolutePath(), null);
                }
            }

            File csvFile = new File(downloadDir, fileName);
            fos = new FileOutputStream(csvFile);
            writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8);
            writer.write(content);
            writer.flush();

            try {
                MediaScannerConnection.scanFile(context,
                        new String[]{csvFile.getAbsolutePath()},
                        null, null);
            } catch (Exception e) {
                ErrorHandler.logWarning(TAG, "MediaScanner scan failed", e);
            }

            return Uri.fromFile(csvFile);

        } catch (Exception e) {
            ErrorHandler.logError(TAG, "保存文件失败（传统方式）", e);
            return null;
        } finally {
            ErrorHandler.closeQuietly(writer);
            ErrorHandler.closeQuietly(fos);
        }
    }

    public static class DateRangeOption {
        public static final int ALL = 0;
        public static final int LAST_3_MONTHS = 1;
        public static final int LAST_6_MONTHS = 2;
        public static final int THIS_YEAR = 3;
        public static final int CUSTOM = 4;
    }

    public long[] getTimeRange(int option, Long customStartDate, Long customEndDate) {
        try {
            long now = System.currentTimeMillis();
            long startTime;
            long endTime = now;

            Calendar calendar = Calendar.getInstance(Locale.CHINA);
            calendar.setTimeInMillis(now);

            switch (option) {
                case DateRangeOption.LAST_3_MONTHS:
                    calendar.add(Calendar.MONTH, -3);
                    startTime = calendar.getTimeInMillis();
                    break;
                case DateRangeOption.LAST_6_MONTHS:
                    calendar.add(Calendar.MONTH, -6);
                    startTime = calendar.getTimeInMillis();
                    break;
                case DateRangeOption.THIS_YEAR:
                    calendar.set(Calendar.MONTH, 0);
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    calendar.set(Calendar.MILLISECOND, 0);
                    startTime = calendar.getTimeInMillis();
                    break;
                case DateRangeOption.CUSTOM:
                    startTime = customStartDate != null ? customStartDate : 0;
                    endTime = customEndDate != null ? customEndDate : now;
                    if (startTime > endTime) {
                        long temp = startTime;
                        startTime = endTime;
                        endTime = temp;
                    }
                    break;
                case DateRangeOption.ALL:
                default:
                    startTime = 0;
                    break;
            }

            return new long[]{startTime, endTime};
        } catch (Exception e) {
            ErrorHandler.logError(TAG, "getTimeRange failed", e);
            return new long[]{0, System.currentTimeMillis()};
        }
    }

    public interface ExportCallback {
        void onSuccess(String fileName, Uri fileUri);
        void onError(String errorMessage);
    }
}