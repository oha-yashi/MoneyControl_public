package com.example.moneycontrol;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class MoneyTableOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "MC.db";
    private static final String[] DATABASE_TITLE = {
            "_id integer primary key autoincrement", //_id行が要るらしい
            "timestamp DEFAULT (datetime('now','localtime'))", //毎回設定しなくてもこれでタイムスタンプが入る
            "income", "outgo", "balance", "wallet", "genre", "note"
    };

    public static final String TABLE_NAME = "MoneyDatabase";
    public static final String READ_ALL_QUERY = "SELECT * FROM " + TABLE_NAME;
    public static final String SQL_CREATE_QUERY = "CREATE TABLE " + TABLE_NAME + " (" + String.join(", ", DATABASE_TITLE) + ")";
    public static final String SQL_DELETE_QUERY = "DROP TABLE " + TABLE_NAME;

    public MoneyTableOpenHelper(Context context) { super(context, DATABASE_NAME, null, DATABASE_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d("SQL", "onCreate");//インストール後初回（データ削除後）だけ動いている
        db.execSQL(SQL_CREATE_QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // アップデート処理
        Log.d("SQL", "Update");
        try{
            db.execSQL(SQL_DELETE_QUERY);
        }
        catch(SQLException e){
            Log.d("UpgradeError", e.toString());
        }

        onCreate(db);
    }

    /**
     * 新しくデータベースを渡す
     * @param context this
     * @return sqLiteDatabase_writable
     */
    public static SQLiteDatabase newDatabase(Context context){
        return new MoneyTableOpenHelper(context).getWritableDatabase();
    }

    /**
     * テーブルの新しい方から最大lines個SELECT*する
     * @param sqLiteDatabase db
     * @param lines SELECT個数
     * @return Cursor
     */
    public static Cursor getNewData(SQLiteDatabase sqLiteDatabase, int lines){
        return sqLiteDatabase.rawQuery("SELECT * FROM "+TABLE_NAME+" ORDER BY _id DESC LIMIT "+lines, null);
    }

    /**
     * walletの残高を出す
     * @param context this
     * @param wallet
     * @return balance of wallet
     */
    public static int getBalanceOf(Context context, String wallet){
        SQLiteDatabase sqLiteDatabase = newDatabase(context);
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT balance FROM MoneyDatabase WHERE wallet=? ORDER BY _id DESC LIMIT 1", new String[]{wallet});
        cursor.moveToFirst();
        //cursor.close(); sqLiteDatabase.close();
        return cursor.getCount()==1 ? cursor.getInt(0) : 0;
    }
}