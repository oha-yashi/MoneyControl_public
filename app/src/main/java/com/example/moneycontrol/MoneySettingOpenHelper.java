package com.example.moneycontrol;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MoneySettingOpenHelper extends SQLiteOpenHelper {
    private static final String TAG = "MSOH";

    private static final int DATABASE_VERSION = 3 ;
    private static final String DATABASE_NAME = "MS.db";

    public static final int INCOME = 0, OUTGO = 1, WALLET = 2;
    public static final String[] TABLE_NAME;
    //ここ1行にしたいけどなんかエラー出る
    static {
        TABLE_NAME = new String[]{"IncomeGenre", "OutgoGenre", "Wallet"};
    }

    public static final String[][] DEFAULT_LIST = {
            {"給料", "残高", "その他"},
            {"食費", "生活費", "娯楽", "交通費", "貯金", "その他"},
            {"財布", "三井住友", "モバイルSuica", "楽天", "ゆうちょ", "その他"}
    };
    String createQuery(int item){
        return "CREATE TABLE "+ TABLE_NAME[item] +" (_id integer primary key autoincrement, name)";
    }

    public MoneySettingOpenHelper(@Nullable Context context) { super(context, DATABASE_NAME, null, DATABASE_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG, "onCreate");

        ContentValues cv = new ContentValues();

        for(int i=INCOME; i<=WALLET; i++){
            sqLiteDatabase.execSQL(createQuery(i));
            for (String s: DEFAULT_LIST[i]) {
                cv.put("name", s);
                sqLiteDatabase.insert(TABLE_NAME[i], null, cv);
                cv.clear();
            }
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        for(int j=INCOME; j<=WALLET; j++) {
            sqLiteDatabase.execSQL("DROP TABLE " + TABLE_NAME[j]);
        }
        onCreate(sqLiteDatabase);
    }

    public static SQLiteDatabase databaseNullCheck(Context context, @org.jetbrains.annotations.Nullable SQLiteDatabase sqLiteDatabase){
        Log.d(TAG, "nullCheck");
        if(sqLiteDatabase==null){
            return new MoneySettingOpenHelper(context).getWritableDatabase();
        }else{
            return sqLiteDatabase;
        }
    }

    /**
     *
     * @param context this
     * @param item INCOME = 0, OUTGO = 1, WALLET = 2;
     * @return
     */
    public static String[] getList(Context context, int item){
        List<String> list = new ArrayList<>();
        String table = TABLE_NAME[item];
        Cursor c = MoneySettingOpenHelper.databaseNullCheck(context, null).rawQuery(
                "SELECT name FROM " + table, null
        );
        c.moveToFirst();
        for(int i=0; i<c.getCount(); i++){
            list.add(c.getString(0));
            c.moveToNext();
        }
        c.close();
        return list.toArray(new String[0]);
    }
}
