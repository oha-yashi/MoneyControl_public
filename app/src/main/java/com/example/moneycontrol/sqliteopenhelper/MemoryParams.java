package com.example.moneycontrol.sqliteopenhelper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.util.Locale;

public class MemoryParams {
    private String MEMORY_TABLE_NAME = "memory_params";

    private final Context context;

    MemoryParams(Context context){
        Log.d("MemoryParams","Constructor");
        this.context = context;
    }

    public void add(InsertParams insertParams){
        try(SQLiteDatabase db = MoneyTable.newDatabase(context)){
            db.execSQL(MoneyTable.QUERY_CREATE(MEMORY_TABLE_NAME));
            db.insert(MEMORY_TABLE_NAME, null, insertParams.toContentValues());
        }
    }

    public String getListTextOf(InsertParams insertParams){
        String note = insertParams.getCombinedNote();
        return String.format(Locale.US,"-%d for %s from %s",insertParams.outgo,note,insertParams.wallet);
    }
}
