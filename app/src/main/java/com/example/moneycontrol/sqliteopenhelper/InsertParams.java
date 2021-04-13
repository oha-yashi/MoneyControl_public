package com.example.moneycontrol.sqliteopenhelper;

import android.database.Cursor;

import androidx.annotation.Nullable;

import com.example.moneycontrol.myTool;

import java.util.Calendar;

/**
 * Insertするのに必要なパラメータ
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
        this.calendar = calendar;
        if(calendar == null)this.calendar = Calendar.getInstance();
        this.income = income;
        this.outgo = outgo;
        this.balance = balance;
        this.wallet = wallet;
        this.genre = genre;
        this.note = note;
//            Log.d("InsertParams", toString());
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
        StringBuilder builder = new StringBuilder();
        builder.append(myTool.toTimestamp(calendar)).append(",")
                .append(income).append(",")
                .append(outgo).append(",")
                .append(balance).append(",")
                .append(wallet).append(",")
                .append(genre).append(",")
                .append(note);
        return builder.toString();
    }
}
