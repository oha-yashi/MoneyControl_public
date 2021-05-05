package com.example.moneycontrol.dbTools;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.Nullable;

import com.example.moneycontrol.MyTool;
import com.example.moneycontrol.sqliteopenhelper.MoneyTable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MemoryParams {
    public static final String MEMORY_TABLE_NAME = "memory_params";

    private final Context context;

    public static final String QUERY_GET = "SELECT * FROM "+MEMORY_TABLE_NAME+" ORDER BY timestamp DESC";

    public MemoryParams(Context context) {
        Log.d("MemoryParams", "Constructor");
        this.context = context;
        MoneyTable.newDatabase(context).execSQL(MoneyTable.QUERY_CREATE(MEMORY_TABLE_NAME));
    }

    public void add(InsertParams insertParams){
        try(SQLiteDatabase db = MoneyTable.newDatabase(context)){
            db.execSQL(MoneyTable.QUERY_CREATE(MEMORY_TABLE_NAME));
            db.insert(MEMORY_TABLE_NAME,null,insertParams.toContentValues());
        }
    }

    public SQLiteDatabase getDB(){
        return MoneyTable.newDatabase(context);
    }

//    public Cursor getListCursor(){
//        try(SQLiteDatabase sqLiteDatabase = getDB()){
//            Cursor c = sqLiteDatabase.rawQuery(MemoryParams.QUERY_GET,null);
//            return c;
//        }
//    }

    public List<String> getList(){
        List<String> list = new ArrayList<>();
        try(SQLiteDatabase sqLiteDatabase = getDB()){
            Cursor c = sqLiteDatabase.rawQuery(MemoryParams.QUERY_GET,null);
            c.moveToFirst();
            for(int i=0; i<c.getCount(); i++){
                list.add(getListTextOf(new InsertParams(c)));
                c.moveToNext();
            }
            c.close();
        }
        return list;
    }

    public InsertParams getParams(int index){
        try(SQLiteDatabase sqLiteDatabase = getDB()){
            Cursor c = sqLiteDatabase.rawQuery(MemoryParams.QUERY_GET,null);
            c.moveToPosition(index);
            return new InsertParams(c);
        }
    }

    @Nullable public static String getListTextOf(InsertParams insertParams){
        String note = insertParams.getCombinedNote();
        String rtn;
        if(MyTool.isHavePlusValue(insertParams.income))rtn = String.format(Locale.US,"+%d for %s from %s",insertParams.income,note,insertParams.wallet);
        else if(MyTool.isHavePlusValue(insertParams.outgo))rtn = String.format(Locale.US,"-%d for %s from %s",insertParams.outgo,note,insertParams.wallet);
        else rtn = null;
        return rtn;
    }
}
