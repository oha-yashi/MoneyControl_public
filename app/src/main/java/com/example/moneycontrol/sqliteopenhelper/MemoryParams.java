package com.example.moneycontrol.sqliteopenhelper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Locale;

public class MemoryParams {
    private static final String MEMORY_TABLE_NAME = "memory_params";

    private final Context context;

    public static final String QUERY_GET = "SELECT * FROM "+MEMORY_TABLE_NAME+" ORDER BY timestamp DESC";

    public MemoryParams(Context context){
        Log.d("MemoryParams","Constructor");
        this.context = context;
        MoneyTable.newDatabase(context).execSQL(MoneyTable.QUERY_CREATE(MEMORY_TABLE_NAME));
    }

    public void add(InsertParams insertParams){
        try(SQLiteDatabase db = MoneyTable.newDatabase(context)){
            db.execSQL(MoneyTable.QUERY_CREATE(MEMORY_TABLE_NAME));
        }
    }

    public SQLiteDatabase getDB(){
        return MoneyTable.newDatabase(context);
    }

    public String[] getList(Cursor c){
        ArrayList<String> list = new ArrayList<>();
        c.moveToFirst();
        for(int i=0; i<c.getCount(); i++){
            list.add(getListTextOf(new InsertParams(c)));
            c.moveToNext();
        }
        return list.toArray(new String[0]);
    }

    @Nullable public String getListTextOf(InsertParams insertParams){
        String note = insertParams.getCombinedNote();
        String rtn;
        if(insertParams.income > 0)rtn = String.format(Locale.US,"+%d for %s from %s",insertParams.income,note,insertParams.wallet);
        else if(insertParams.outgo > 0)rtn = String.format(Locale.US,"-%d for %s from %s",insertParams.outgo,note,insertParams.wallet);
        else rtn = null;
        return rtn;
    }
}
