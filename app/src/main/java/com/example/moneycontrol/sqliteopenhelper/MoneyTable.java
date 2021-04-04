package com.example.moneycontrol.sqliteopenhelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MoneyTable extends SQLiteOpenHelper {

    private static final int IOM_INCOME = 1;
    private static final int IOM_OUTGO = 2;
    private static final int IOM_MOVE = 3;

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "MC.db";
    private static final String[] DATABASE_COLUMNS = {
            "_id integer primary key autoincrement", //_id行が要るらしい
            "timestamp DEFAULT (datetime('now','localtime'))", //毎回設定しなくてもこれでタイムスタンプが入る
            "income", "outgo", "balance", "wallet", "genre", "note"
    };

    private static String TABLE_NAME = getTodayTableName(); //ここで一応宣言時代入ができている。
    public static final String READ_ALL_QUERY = "SELECT * FROM " + TABLE_NAME;
    public static final String SQL_CREATE_QUERY = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" + String.join(", ", DATABASE_COLUMNS) + ")";
    public static final String SQL_DELETE_QUERY = "DROP TABLE " + TABLE_NAME;

    public static String getTodayTableName(){
        return getCalendarTableName(Calendar.getInstance());
    }
    public static String getCalendarTableName(Calendar calendar){
        SimpleDateFormat sdf = new SimpleDateFormat("'Y'yyyy'M'MM", Locale.US);
        return sdf.format(calendar.getTime());
    }
    public static void setTableName(String tablename){
        TABLE_NAME = tablename;
    }
    public static String getExistTableNames(Context context){
        String GET_TABLENAME_QUERY = "SELECT name FROM sqlite_master WHERE type ='table' AND name NOT LIKE 'sqlite_%'";
        SQLiteDatabase sqLiteDatabase = newDatabase(context);
        Cursor cursor = sqLiteDatabase.rawQuery(GET_TABLENAME_QUERY, null);
        StringBuilder stringBuilder = new StringBuilder();
        cursor.moveToFirst();
        for(int i=0; i<cursor.getCount(); i++){
            stringBuilder.append(cursor.getString(0)).append("/");
            cursor.moveToNext();
        }
        cursor.close();
        sqLiteDatabase.close();
        return stringBuilder.toString();
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

    public MoneyTable(Context context) {
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
     * 更新されたテーブル名の存在チェックもする（クエリで処理）
     * @param context this
     * @return sqLiteDatabase
     */
    public static SQLiteDatabase newDatabase(Context context){
        SQLiteDatabase sqLiteDatabase = new MoneyTable(context).getWritableDatabase();
        sqLiteDatabase.execSQL(SQL_CREATE_QUERY);
        return  sqLiteDatabase;
    }

    /**
     * テーブルのTIMESTAMP新しい方から最大lines個SELECT*する
     * @param sqLiteDatabase db
     * @param lines SELECT個数
     * @return Cursor
     */
    public static Cursor getNewTimeData(SQLiteDatabase sqLiteDatabase, int lines){
        return sqLiteDatabase.rawQuery("SELECT * FROM "+TABLE_NAME+" ORDER BY timestamp DESC LIMIT "+lines, null);
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
        sum = c.getInt(0);
        c.close();

        db.close();
        return sum;
    }

    public static int monthAverage(Context context){
        int sum = 0;
        SQLiteDatabase db = newDatabase(context);
        String INCOME_SUM_QUERY = "SELECT total(outgo) FROM "+TABLE_NAME;
        Cursor c = db.rawQuery(INCOME_SUM_QUERY, null);
        c.moveToFirst();
        sum = c.getInt(0);
        c.close();
        db.close();
        int days = Calendar.getInstance().get(Calendar.DATE);
        return sum/days;
    }

    public static void setWithTime(Context context, Calendar time, int income,
                                   int outgo, int balance, String wallet, String genre, String note){

        SQLiteDatabase db = newDatabase(context);
        if(time==null)time = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String strTime = sdf.format(time.getTime());
        Log.d("setWithTime", "set timestamp "+strTime);

        ContentValues cv = new ContentValues();

        cv.put("timestamp", strTime);
        cv.put("income", income);
        cv.put("outgo", outgo);
        cv.put("balance", balance);
        cv.put("wallet", wallet);
        cv.put("genre", genre);
        cv.put("note", note);

        db.close();
    }
}

