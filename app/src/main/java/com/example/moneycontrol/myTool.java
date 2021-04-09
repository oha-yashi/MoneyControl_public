package com.example.moneycontrol;

import android.database.Cursor;
import android.text.TextUtils;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * 各class固有でもない、使える関数とかを置いておく
 * 他のところで使うものなのでpublic宣言すること。
 */
public class myTool {
    /**
     * cursorからnullかもしれないStringの読み込み
     * cursor.getString(i) -> getNullableString(cursor, i)
     * @param cursor
     * @param index
     * @return
     */
    public static String getNullableString(Cursor cursor, int index){
        String rtn = cursor.getString(index);
        if(rtn == null) rtn = "";
        else rtn = rtn.trim();
        return rtn;
    }

    public static int getNullableInt(EditText editText){
        String rtn = editText.getText().toString();
        if(TextUtils.isEmpty(rtn))return 0;
        else return Integer.parseInt(rtn);
    }

    /**
     * calendarからSQLite式タイムスタンプに変換
     * @param calendar
     * @return timestamp
     */
    public static String calendarToTimestamp(@NotNull Calendar calendar){
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        String rtn = simpleDateFormat.format(calendar.getTime());
        return rtn;
    }
}
