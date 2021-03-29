package com.example.moneycontrol;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MoneyTableOpenHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "MC.db";
    private static final String[] DATABASE_COLUMNS = {
            "_id integer primary key autoincrement", //_id行が要るらしい
            "timestamp DEFAULT (datetime('now','localtime'))", //毎回設定しなくてもこれでタイムスタンプが入る
            "income", "outgo", "balance", "wallet", "genre", "note"
    };
    private static final boolean isDebug = true;//TODO: getTableNameの切替え。falseにすると可変になる

    private static final String TABLE_NAME = getTableName(); //ここで宣言時代入ができている。
    public static final String READ_ALL_QUERY = "SELECT * FROM " + TABLE_NAME;
    public static final String SQL_CREATE_QUERY = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + String.join(", ", DATABASE_COLUMNS) + ")";
    public static final String SQL_DELETE_QUERY = "DROP TABLE " + TABLE_NAME;

    public static String getTableName(){
        return isDebug ? "MoneyDatabase" : "Y"+ Calendar.getInstance().get(Calendar.YEAR) +"M"+(Calendar.getInstance().get(Calendar.MONTH)+1);
    }

    /**
     * カラム名の配列を返す
     * @return colmuns as StringArray
     */
    public static String[] getColumnsArray(){
        List<String> list = new ArrayList<>();
        for(String column: DATABASE_COLUMNS){
            int spaceIndex = column.indexOf(" ");
            if(spaceIndex == -1){
                list.add(column);
            }else{
                list.add(column.substring(0, spaceIndex));
            }
        }
        return list.toArray(new String[0]);
    }

    /**
     * カラム名を , 区切りで文字列にして出す
     * @return joinedColumns
     */
    public static String getColumnsJoined(){
        return String.join(",", getColumnsArray());
    }

    public MoneyTableOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

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
     * 新しくデータベースを渡す。いつもここから。
     * 更新されたテーブル名の存在チェックもする
     * @param context this
     * @return sqLiteDatabase_writable
     */
    public static SQLiteDatabase newDatabase(Context context){
        SQLiteDatabase sqLiteDatabase = new MoneyTableOpenHelper(context).getWritableDatabase();
        sqLiteDatabase.execSQL(SQL_CREATE_QUERY);
        return  sqLiteDatabase;
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
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT balance FROM "+TABLE_NAME+" WHERE wallet=? ORDER BY _id DESC LIMIT 1", new String[]{wallet});
        cursor.moveToFirst();
        //cursor.close(); sqLiteDatabase.close();
        return cursor.getCount()==1 ? cursor.getInt(0) : 0;
    }

    /**
     * 今日の支出を出す
     * @return int 支出
     */
    public static int todaySum(Context context){
        int sum = 0;
        SQLiteDatabase db = newDatabase(context);

        String SEARCH_TODAYSUM_QUERY = "select total(outgo) from " + TABLE_NAME
                + " where strftime('%m%d', timestamp) = strftime('%m%d', 'now', 'localtime')";
        Cursor c = db.rawQuery(SEARCH_TODAYSUM_QUERY, null);
        c.moveToFirst();
        for(int i=0; i<c.getCount(); i++)sum += c.getInt(0);
        c.close();

        db.close();
        return sum;
    }
}