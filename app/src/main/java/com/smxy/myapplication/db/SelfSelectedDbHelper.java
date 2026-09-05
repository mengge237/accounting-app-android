package com.smxy.myapplication.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SelfSelectedDbHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "self_selected.db";
    private static final int DATABASE_VERSION = 1;

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + SelfSelectedContract.SelfSelectedEntry.TABLE_NAME + " (" +
                    SelfSelectedContract.SelfSelectedEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                    SelfSelectedContract.SelfSelectedEntry.COLUMN_CODE + " TEXT NOT NULL," +
                    SelfSelectedContract.SelfSelectedEntry.COLUMN_NAME + " TEXT NOT NULL," +
                    SelfSelectedContract.SelfSelectedEntry.COLUMN_TYPE + " TEXT DEFAULT '股票'," +
                    SelfSelectedContract.SelfSelectedEntry.COLUMN_BUY_PRICE + " REAL," +
                    SelfSelectedContract.SelfSelectedEntry.COLUMN_CURRENT_PRICE + " REAL," +
                    SelfSelectedContract.SelfSelectedEntry.COLUMN_REMARK + " TEXT," +
                    SelfSelectedContract.SelfSelectedEntry.COLUMN_CREATE_TIME + " INTEGER)";

    public SelfSelectedDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + SelfSelectedContract.SelfSelectedEntry.TABLE_NAME);
        onCreate(db);
    }
}