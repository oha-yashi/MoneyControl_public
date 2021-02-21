package com.example.moneycontrol;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MCOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "MC.db";
    private static final String[] DATABASE_TITLE = {
            "_id integer primary key autoincrement", //_id行が要るらしい
            "timestamp DEFAULT (datetime('now','localtime'))", //毎回設定しなくてもこれでタイムスタンプが入る
            "status", "money", "wallet", "genre", "note"
    };

    public static final String INCOME_GENRE_TABLE = "IncomeGenre";
    private static final String IG_CREATE = "create table " + INCOME_GENRE_TABLE + " (_id integer primary key autoincrement, name)";
    public static final String[] IG_LIST = {"収入", "残高", "その他"};

    public static final String OUTGO_GENRE_TABLE = "OutgoGenre";
    private static final String OG_CREATE = "create table " + OUTGO_GENRE_TABLE + " (_id integer primary key autoincrement, name)";
    public static final String[] OG_LIST = {"食費", "生活費", "娯楽", "交通費", "貯金", "その他"};

    public static final String WALLET_TABLE = "Wallet";
    private static final String WT_CREATE = "create table " + WALLET_TABLE + " (_id integer primary key autoincrement, name, balance DEFAULT 0)";
    public static final String[] WALLET_LIST = {"財布", "三井住友", "モバイルSuica", "楽天", "その他"};

    public MCOpenHelper(Context context) { super(context, DATABASE_NAME, null, DATABASE_VERSION); }

    public String TABLE_NAME = "MoneyDatabase";
    public String READ_ALL_QUERY = "select * from " + TABLE_NAME;
    public String SQL_DELETE_QUERY = "DELETE FROM " + TABLE_NAME;

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("SQL", "onCreate");//インストール後初回（データ削除後）だけ動いている
        String SQL_CREATE_QUERY = "create table " + TABLE_NAME + " (" + String.join(", ", DATABASE_TITLE) + ")";

        db.execSQL(SQL_CREATE_QUERY);

        ContentValues cv = new ContentValues();

        db.execSQL(IG_CREATE);
        for (String s:IG_LIST) {
            cv.put("name", s);
            db.insert(INCOME_GENRE_TABLE, null, cv);
            cv.clear();
        }

        db.execSQL(OG_CREATE);
        for (String s:OG_LIST) {
            cv.put("name", s);
            db.insert(OUTGO_GENRE_TABLE, null, cv);
            cv.clear();
        }

        db.execSQL(WT_CREATE);
        for (String s:WALLET_LIST) {
            cv.put("name", s);
            db.insert(WALLET_TABLE, null, cv);
            cv.clear();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // アップデート処理
        Log.d("SQL", "Update");
        db.execSQL(SQL_DELETE_QUERY);
        onCreate(db);
    }

    /**
     * データベースの単一列を抜き出す
     * @param db データベース
     * @param table テーブル名
     * @param column 列名
     * @return データのリスト
     */
    public List<String> tableColumnToArray(SQLiteDatabase db, String table, @NotNull String column){
        List<String> l = new ArrayList<>();
        Cursor c = db.rawQuery("SELECT " + column + " FROM " + table, null);
        c.moveToFirst();
        for(int i=0; i<c.getCount(); i++){
            l.add(c.getString(0));
            c.moveToNext();
        }
        c.close();
        return l;
    }
}