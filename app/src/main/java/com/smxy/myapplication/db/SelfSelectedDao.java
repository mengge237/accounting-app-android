package com.smxy.myapplication.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.smxy.myapplication.model.SelfSelectedItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 股票自选数据访问对象
 */
public class SelfSelectedDao {
    private SelfSelectedDbHelper dbHelper;

    public SelfSelectedDao(Context context) {
        dbHelper = new SelfSelectedDbHelper(context);
    }

    /**
     * 添加自选项目
     */
    public long insert(SelfSelectedItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_CODE, item.getCode());
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_NAME, item.getName());
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_TYPE, item.getType());
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_BUY_PRICE, item.getBuyPrice());
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_CURRENT_PRICE, item.getCurrentPrice());
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_REMARK, item.getRemark());
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_CREATE_TIME, item.getCreateTime());
        long id = db.insert(SelfSelectedContract.SelfSelectedEntry.TABLE_NAME, null, values);
        db.close();
        return id;
    }

    /**
     * 删除自选项目
     */
    public int delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(SelfSelectedContract.SelfSelectedEntry.TABLE_NAME,
                SelfSelectedContract.SelfSelectedEntry._ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /**
     * 更新自选项目
     */
    public int update(SelfSelectedItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_CODE, item.getCode());
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_NAME, item.getName());
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_TYPE, item.getType());
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_BUY_PRICE, item.getBuyPrice());
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_CURRENT_PRICE, item.getCurrentPrice());
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_REMARK, item.getRemark());
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_CREATE_TIME, item.getCreateTime());
        int rows = db.update(SelfSelectedContract.SelfSelectedEntry.TABLE_NAME, values,
                SelfSelectedContract.SelfSelectedEntry._ID + " = ?",
                new String[]{String.valueOf(item.getId())});
        db.close();
        return rows;
    }

    /**
     * 更新当前价
     */
    public int updateCurrentPrice(long id, double currentPrice) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(SelfSelectedContract.SelfSelectedEntry.COLUMN_CURRENT_PRICE, currentPrice);
        int rows = db.update(SelfSelectedContract.SelfSelectedEntry.TABLE_NAME, values,
                SelfSelectedContract.SelfSelectedEntry._ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
        return rows;
    }

    /**
     * 查询所有自选项目
     */
    public List<SelfSelectedItem> getAll() {
        List<SelfSelectedItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(SelfSelectedContract.SelfSelectedEntry.TABLE_NAME,
                null, null, null, null, null,
                SelfSelectedContract.SelfSelectedEntry.COLUMN_CREATE_TIME + " DESC");

        while (cursor.moveToNext()) {
            SelfSelectedItem item = cursorToItem(cursor);
            list.add(item);
        }
        cursor.close();
        db.close();
        return list;
    }

    /**
     * 根据ID查询
     */
    public SelfSelectedItem getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(SelfSelectedContract.SelfSelectedEntry.TABLE_NAME,
                null,
                SelfSelectedContract.SelfSelectedEntry._ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null);

        SelfSelectedItem item = null;
        if (cursor.moveToFirst()) {
            item = cursorToItem(cursor);
        }
        cursor.close();
        db.close();
        return item;
    }

    /**
     * 根据代码查询是否已存在
     */
    public boolean exists(String code) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(SelfSelectedContract.SelfSelectedEntry.TABLE_NAME,
                new String[]{SelfSelectedContract.SelfSelectedEntry._ID},
                SelfSelectedContract.SelfSelectedEntry.COLUMN_CODE + " = ?",
                new String[]{code},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }

    /**
     * 获取数量
     */
    public int getCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(SelfSelectedContract.SelfSelectedEntry.TABLE_NAME,
                new String[]{SelfSelectedContract.SelfSelectedEntry._ID},
                null, null, null, null, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count;
    }

    /**
     * 清空所有
     */
    public int deleteAll() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(SelfSelectedContract.SelfSelectedEntry.TABLE_NAME, null, null);
        db.close();
        return rows;
    }

    private SelfSelectedItem cursorToItem(Cursor cursor) {
        SelfSelectedItem item = new SelfSelectedItem();
        item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(SelfSelectedContract.SelfSelectedEntry._ID)));
        item.setCode(cursor.getString(cursor.getColumnIndexOrThrow(SelfSelectedContract.SelfSelectedEntry.COLUMN_CODE)));
        item.setName(cursor.getString(cursor.getColumnIndexOrThrow(SelfSelectedContract.SelfSelectedEntry.COLUMN_NAME)));
        item.setType(cursor.getString(cursor.getColumnIndexOrThrow(SelfSelectedContract.SelfSelectedEntry.COLUMN_TYPE)));
        item.setBuyPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(SelfSelectedContract.SelfSelectedEntry.COLUMN_BUY_PRICE)));
        item.setCurrentPrice(cursor.getDouble(cursor.getColumnIndexOrThrow(SelfSelectedContract.SelfSelectedEntry.COLUMN_CURRENT_PRICE)));
        item.setRemark(cursor.getString(cursor.getColumnIndexOrThrow(SelfSelectedContract.SelfSelectedEntry.COLUMN_REMARK)));
        item.setCreateTime(cursor.getLong(cursor.getColumnIndexOrThrow(SelfSelectedContract.SelfSelectedEntry.COLUMN_CREATE_TIME)));
        return item;
    }
}