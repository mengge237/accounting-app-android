package com.smxy.myapplication.db;

import android.provider.BaseColumns;

public final class SelfSelectedContract {
    private SelfSelectedContract() {}

    public static class SelfSelectedEntry implements BaseColumns {
        public static final String TABLE_NAME = "self_selected";
        public static final String COLUMN_CODE = "code";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_TYPE = "type";
        public static final String COLUMN_BUY_PRICE = "buy_price";
        public static final String COLUMN_CURRENT_PRICE = "current_price";
        public static final String COLUMN_REMARK = "remark";
        public static final String COLUMN_CREATE_TIME = "create_time";
    }
}