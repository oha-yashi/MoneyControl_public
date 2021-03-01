package com.example.moneycontrol;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MoneyTableOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "MC.db";
    private static final String[] DATABASE_TITLE = {
            "_id integer primary key autoincrement", //_id行が要るらしい
            "timestamp DEFAULT (datetime('now','localtime'))", //毎回設定しなくてもこれでタイムスタンプが入る
            "status", "money", "wallet", "genre", "note"
    };

    public static final String TABLE_NAME = "MoneyDatabase";
    public static final String READ_ALL_QUERY = "SELECT * FROM " + TABLE_NAME;
    public static final String SQL_DELETE_QUERY = "DELETE FROM " + TABLE_NAME;

    public MoneyTableOpenHelper(Context context) { super(context, DATABASE_NAME, null, DATABASE_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("SQL", "onCreate");//インストール後初回（データ削除後）だけ動いている
        String SQL_CREATE_QUERY = "create table " + TABLE_NAME + " (" + String.join(", ", DATABASE_TITLE) + ")";

        db.execSQL(SQL_CREATE_QUERY);


    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // アップデート処理
        Log.d("SQL", "Update");
        db.execSQL(SQL_DELETE_QUERY);
        onCreate(db);
    }

    /*
      データベースの単一列を抜き出す
      @param database データベース
     * @param table テーブル名
     * @param column 列名
     * @return データのリスト
     */
    /* 閉鎖します
    public static String[] tableColumnToArray(SQLiteDatabase database, String table, @NotNull String column){
        List<String> l = new ArrayList<>();
        Cursor c = database.rawQuery("SELECT " + column + " FROM " + table, null);
        c.moveToFirst();
        for(int i=0; i<c.getCount(); i++){
            l.add(c.getString(0));
            c.moveToNext();
        }
        c.close();
        return (String[]) l.toArray();
    }
    //*/

    /**
     * 引数のデータベースがnullのとき新しく作って返す。
     * nullを渡して初期化にも使える
     * @param context e.g.(this
     * @param sqLiteDatabase database
     * @return sqLiteDatabase_writable
     */
    public static SQLiteDatabase databaseNullCheck(Context context, @Nullable SQLiteDatabase sqLiteDatabase){
        Log.d("DBNullCheck", "run");
        if(sqLiteDatabase==null){
            Log.d("DBNullCheck", "is null");
            return new MoneyTableOpenHelper(context).getWritableDatabase();
        }else{
            return sqLiteDatabase;
        }
    }
}