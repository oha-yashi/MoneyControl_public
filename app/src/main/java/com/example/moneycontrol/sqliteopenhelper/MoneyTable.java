package com.example.moneycontrol.sqliteopenhelper;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MoneyTable extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 2;
    private static final String DATABASE_NAME = "MC.db";
    private static final String[] DATABASE_COLUMNS = {
            "_id integer primary key autoincrement", //_id行が要るらしい
            "timestamp DEFAULT (datetime('now','localtime'))", //毎回設定しなくてもこれでタイムスタンプが入る
            "income", "outgo", "balance", "wallet", "genre", "note"
    };

    //ここで一応宣言時代入ができている。デフォルトはgetTodayTableName
    private static String TABLE_NAME = getTodayTableName();
    public static String QUERY_SELECT_ALL(String tableName) {return "SELECT * FROM " + tableName;}
    public static String QUERY_CREATE(String tableName) {return "CREATE TABLE IF NOT EXISTS " + tableName + " (" + String.join(", ", DATABASE_COLUMNS) + ")";}
    public static String QUERY_DELETE(String tableName, int id) {return "DELETE FROM " + tableName + " WHERE _id=" + id;}
    public static String QUERY_DROP(String tableName) {return "DROP TABLE " + tableName;}

    public static String getTodayTableName(){
        return getCalendarTableName(Calendar.getInstance());
    }
    public static String getCalendarTableName(Calendar calendar){
        SimpleDateFormat sdf = new SimpleDateFormat("'Y'yyyy'M'MM", Locale.US);
        return sdf.format(calendar.getTime());
    }
    public static String getExistTableNames(Context context){
        String QUERY_GET_TABLES = "SELECT name FROM sqlite_master WHERE type ='table' AND name NOT LIKE 'sqlite_%'";
        StringBuilder stringBuilder = new StringBuilder();
        try(SQLiteDatabase sqLiteDatabase = newDatabase(context)) {
            Cursor cursor = sqLiteDatabase.rawQuery(QUERY_GET_TABLES, null);
            cursor.moveToFirst();
            for (int i = 0; i < cursor.getCount(); i++) {
                stringBuilder.append(cursor.getString(0)).append("/");
                cursor.moveToNext();
            }
            cursor.close();
        }
        return stringBuilder.toString();
    }

    public static String[] getMoneyTableNames(Context context){
        String QUERY_GET_TABLES = "SELECT name FROM sqlite_master WHERE type ='table' AND name LIKE 'Y%M%'";
        List<String> rtn = new ArrayList<>();
        try(SQLiteDatabase db = newDatabase(context)){
            Cursor cursor = db.rawQuery(QUERY_GET_TABLES, null);
            cursor.moveToFirst();
            for(int i=0; i<cursor.getCount(); i++){
                rtn.add(cursor.getString(0));
                cursor.moveToNext();
            }
            cursor.close();
        }
        return rtn.toArray(new String[0]);
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
        db.execSQL(QUERY_CREATE(TABLE_NAME));
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // アップデート処理
        Log.d("SQL", "Update");
        try{
            db.execSQL(QUERY_DROP(getTodayTableName()));
        }
        catch(SQLException e){
            e.printStackTrace();
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
        sqLiteDatabase.execSQL(QUERY_CREATE(TABLE_NAME));
        return  sqLiteDatabase;
    }

    /**
     * テーブルのTIMESTAMP新しい方から最大lines個SELECT*する
     * @param context
     * @param table_name
     * @param lines SELECT個数 -1 でLIMIT無し
     * @return Cursor
     */
    public static Cursor getNewTimeData(Context context, String table_name, int lines){
        SQLiteDatabase sqLiteDatabase = newDatabase(context);
        String sql = "SELECT * FROM "+table_name+" ORDER BY timestamp DESC";
        if(lines>=0) sql += " LIMIT "+lines;
        return sqLiteDatabase.rawQuery(sql, null);
    }

    /**
     * walletの残高を出す
     * @param context this
     * @param wallet
     * @return balance of wallet
     */
    public static int getBalanceOf(Context context, String wallet){
        int rtn = 0;
        try(SQLiteDatabase sqLiteDatabase = newDatabase(context)){
            Cursor cursor = sqLiteDatabase.rawQuery("SELECT balance FROM "+TABLE_NAME+" WHERE wallet=? ORDER BY timestamp DESC LIMIT 1", new String[]{wallet});
            cursor.moveToFirst();
            rtn = cursor.getCount()==1 ? cursor.getInt(0) : 0;
            cursor.close();
        } catch (Exception e){e.printStackTrace();
        } finally {
            return rtn;
        }
    }

    /**
     * 今日の支出を出す
     * @return int 支出
     */
    public static int todaySum(Context context){
        int sum = 0;
        try(SQLiteDatabase db = newDatabase(context)){
            String SEARCH_TODAYSUM_QUERY = "select total(outgo) from " + TABLE_NAME
                    + " where strftime('%m%d', timestamp) = strftime('%m%d', 'now', 'localtime')";
            Cursor c = db.rawQuery(SEARCH_TODAYSUM_QUERY, null);
            c.moveToFirst();
            sum = c.getInt(0);
            c.close();
        } catch(Exception e){e.printStackTrace();
        } finally {
            return sum;
        }
    }

    public static int monthSum(Context context){
        int sum = 0;
        try(SQLiteDatabase db = newDatabase(context)){
            String INCOME_SUM_QUERY = "SELECT total(outgo) FROM "+TABLE_NAME;
            Cursor c = db.rawQuery(INCOME_SUM_QUERY, null);
            c.moveToFirst();
            sum = c.getInt(0);
            c.close();
        } catch (Exception e){
            e.printStackTrace();
        }finally {
            return sum;
        }
    }

    public static int monthAverage(int monthSum){
        int days = Calendar.getInstance().get(Calendar.DATE);
        return monthSum/days;
    }

    public static int getRecentId(Context context){
        String QUERY = "SELECT _id FROM "+getTodayTableName()+" ORDER BY timestamp DESC LIMIT 1";
        int rtn = -1;
        try(SQLiteDatabase db = newDatabase(context)){
            Cursor cursor = db.rawQuery(QUERY, null);
            cursor.moveToFirst();
            rtn = cursor.getInt(0);
            cursor.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        return rtn;
    }

    /**
     * 削除メソッド
     * @param context
     * @param id
     */
    public static void deleteById(Context context, int id){
        try(SQLiteDatabase db = newDatabase(context)) {
            db.execSQL(QUERY_DELETE(getTodayTableName(),id));
        }
    }

    /**
     *
     * @param context
     * @param tableName
     * @param id
     * @return
     * @throws Exception 存在しないid
     */
    public static String toStringTableId(Context context, String tableName, int id) throws Exception {
        String rtn;
        try (SQLiteDatabase db = newDatabase(context)){
            String QUERY = "SELECT * FROM " + tableName + " WHERE _id=" + id;
            Cursor cursor = db.rawQuery(QUERY, null);

            if(cursor.getCount()==0)throw new Exception("存在しないidです");

            cursor.moveToFirst();
            rtn = new InsertParams(cursor).toString();
            cursor.close();
        }
        return rtn;
    }
}

