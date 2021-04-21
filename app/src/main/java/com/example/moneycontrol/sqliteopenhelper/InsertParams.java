package com.example.moneycontrol.sqliteopenhelper;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.moneycontrol.MainActivity;
import com.example.moneycontrol.myTool;

import java.util.Calendar;

/**
 * Insertするのに必要なパラメータ {calendar, income, outgo, balance, wallet, genre, note}
 */
public class InsertParams {
    public Calendar calendar;
    public Integer income;
    public Integer outgo;
    public Integer balance;
    public String wallet;
    public String genre;
    public String note;

    public InsertParams(@Nullable Calendar calendar,
                        @Nullable Integer income, @Nullable Integer outgo, Integer balance,
                        String wallet, String genre, String note){
        this.calendar = calendar==null ? Calendar.getInstance() : calendar;
        this.income = income;
        this.outgo = outgo;
        this.balance = balance;
        this.wallet = wallet;
        this.genre = genre;
        this.note = note;

//        Log.d("InsertParams", toString());
//        int i = balance + (income==null?0:income) - (outgo==null?0:outgo);
//        Log.d("test", String.valueOf(i));
    }

    public InsertParams(Cursor cursor){
//            _id = cursor.getInt(0);
        this.calendar = myTool.toCalendar(cursor.getString(1));
        this.income = cursor.getInt(2);
        this.outgo = cursor.getInt(3);
        this.balance = cursor.getInt(4);
        this.wallet = myTool.getNullableString(cursor, 5);
        this.genre = myTool.getNullableString(cursor, 6);
        this.note = myTool.getNullableString(cursor, 7);
    }

    public String toString(){
        return myTool.toTimestamp(calendar) + "," +
                income + "," +
                outgo + "," +
                balance + "," +
                wallet + "," +
                genre + "," +
                note;
    }

    public ContentValues toContentValues(){
        ContentValues cv = new ContentValues();
        cv.put("timestamp", myTool.toTimestamp(calendar));
        cv.put("income", income);
        cv.put("outgo", outgo);
        cv.put("balance", balance);
        cv.put("wallet", wallet);
        cv.put("genre", genre);
        cv.put("note", note);
        return cv;
    }

    public String getStatus(){
        return this.income > 0 ? "△" :
                this.outgo > 0 ? "▼" : "";
    }

    public String getCombinedNote(){
        String g = this.genre;
        String n = this.note;
        String rtn;
        if (TextUtils.isEmpty(g) || TextUtils.isEmpty(n)) rtn = String.format("%s%s", g, n);
        else rtn = String.format("%s : %s", g, n);
        return rtn;
    }

    public static Pair<InsertParams,InsertParams> makeMoveParams(
            @Nullable Calendar calendar, @NonNull Integer money,
            @NonNull Integer balance1, @NonNull Integer balance2,
            @NonNull String wallet1, @NonNull String wallet2,
            @Nullable String noteByGenre){
        Log.d("makeMoveParams", "run");
        Calendar c1 = calendar==null ? Calendar.getInstance() : calendar;
        Calendar c2 = Calendar.getInstance();
        c2.setTime(c1.getTime());
        c2.add(Calendar.SECOND,1);
        String noteAdd = TextUtils.isEmpty(noteByGenre) ? "" : " : "+noteByGenre;
        Pair<InsertParams,InsertParams> rtn = new Pair<>(
                new InsertParams(c1,null,null, balance1 - money, wallet1,"資金移動","-"+money+noteAdd),
                new InsertParams(c2,null,null, balance2 + money, wallet2,"資金移動","+"+money+noteAdd)
        );
        return rtn;
    }
}
