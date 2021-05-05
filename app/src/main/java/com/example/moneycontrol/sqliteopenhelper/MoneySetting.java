package com.example.moneycontrol.sqliteopenhelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MoneySetting extends SQLiteOpenHelper {
    private static final String TAG = "MSOH";

    private static final int DATABASE_VERSION = 4 ;
    private static final String DATABASE_NAME = "MS.db";

    public static final int INCOME = 0, OUTGO = 1, WALLET = 2;
    public static String[] TABLE_NAME;
//    なんでこれ1行にならないの
    static {
        TABLE_NAME = new String[]{"IncomeGenre", "OutgoGenre", "Wallet"};
    }

    public static final String[][] DEFAULT_LIST = {
            {"給料", "その他"},
            {"食費", "生活費", "娯楽", "交通費", "通販", "貯金", "その他"},
            {"財布", "三井住友", "モバイルSuica", "楽天", "ゆうちょ", "貯金", "その他"}
    };
    public static String QUERY_CREATE(int i){ return "CREATE TABLE "+ TABLE_NAME[i] +" (_id INTEGER primary key autoincrement, name TEXT, priority INTEGER)"; }
    public static String QUERY_UPDATE(int i, String from, String to){return String.format("UPDATE %s SET name='%s' WHERE name='%s' ", TABLE_NAME[i],to,from);}
    public static String QUERY_DELETE(int i, String name){return String.format("DELETE FROM %s WHERE name='%s'", TABLE_NAME[i],name);}
    public static String[] QUERY_MOVE_PRIORITY(int i, String name, int from, int to){
        int move_other = from > to ? 1 : -1;
        int st = Integer.min(from,to);
        int en = Integer.max(from,to);
        return new String[]{
                String.format(Locale.US,"UPDATE %s SET priority=priority+(%d) WHERE priority>=%d AND priority<%d",
                        TABLE_NAME[i], move_other, st, en),
                String.format(Locale.US,"UPDATE %s SET priority=%d WHERE name='%s'",TABLE_NAME[i],to,name)
        };
    }

    public MoneySetting(@Nullable Context context) { super(context, DATABASE_NAME, null, DATABASE_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        Log.d(TAG, "onCreate");

        ContentValues cv = new ContentValues();

        for(int i=INCOME; i<=WALLET; i++){
            sqLiteDatabase.execSQL(QUERY_CREATE(i));
            int j=0;
            for (String s: DEFAULT_LIST[i]) {
                cv.put("name", s);
                cv.put("priority", j++);
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

    public static SQLiteDatabase databaseNullCheck(Context context, @Nullable SQLiteDatabase sqLiteDatabase){
        Log.d(TAG, "nullCheck");
        if(sqLiteDatabase==null){
            return new MoneySetting(context).getWritableDatabase();
        }else{
            return sqLiteDatabase;
        }
    }

    /**
     * get List by priority
     * @param context this
     * @param item INCOME = 0, OUTGO = 1, WALLET = 2;
     * @return 要素一覧
     */
    public static Pair<List<Integer>,List<String>> getList(Context context, int item){
        List<Integer> listInt = new ArrayList<>();
        List<String> listStr = new ArrayList<>();
        String table = TABLE_NAME[item];
        try(SQLiteDatabase db = databaseNullCheck(context, null)){
            Cursor c = db.rawQuery(String.format("SELECT * FROM %s ORDER BY priority", table), null);
            c.moveToFirst();
            for(int i=0; i<c.getCount(); i++){
                listInt.add(c.getInt(2));
                listStr.add(c.getString(1));
                c.moveToNext();
            }
            c.close();
        } catch (Exception e){
            e.printStackTrace();
        }

        return Pair.create(listInt,listStr);
    }

    /**
     * IncomeとOutgoのgenreを連結して返す
     * @param context
     * @return {"収入",{収入リスト},"支出",{支出リスト}}
     */
    public static List<String> getGenreList(Context context){
        List<String> genreList = new ArrayList<>();
        genreList.add("収入");
        genreList.addAll(getList(context,INCOME).second);
        genreList.add("支出");
        genreList.addAll(getList(context,OUTGO).second);
        return genreList;
    }
}
