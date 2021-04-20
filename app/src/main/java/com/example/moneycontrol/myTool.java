package com.example.moneycontrol;

import android.database.Cursor;
import android.text.TextUtils;
import android.widget.EditText;

import androidx.annotation.NonNull;

import java.text.ParseException;
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
     * cursor.getString(_interface) -> getNullableString(cursor, _interface)
     *
     * @param cursor
     * @param index
     * @return
     */
    public static @NonNull String getNullableString(Cursor cursor, int index) {
        String rtn = cursor.getString(index);
        if (rtn == null) rtn = "";
        else rtn = rtn.trim();
        return rtn;
    }

    public static int getNullableInt(EditText editText) {
        String rtn = editText.getText().toString();
        if (TextUtils.isEmpty(rtn)) return 0;
        else return Integer.parseInt(rtn);
    }

    private static SimpleDateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    /**
     * calendarからSQLite式タイムスタンプに変換
     * @param calendar
     * @return timestamp
     */
    public static String toTimestamp(@NonNull Calendar calendar) {
        return timestampFormat.format(calendar.getTime());
    }

    /**
     * SQLite式タイムスタンプからcalendarに変換
     * @param timestamp
     * @return calendar
     */
    public static Calendar toCalendar(@NonNull String timestamp){
        Calendar rtn = Calendar.getInstance();
        try {
            rtn.setTime(timestampFormat.parse(timestamp));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return rtn;
    }

    /**
     * 関数を渡すために使うクラス 引数無し返り値void
     *
     * f();
     * -->
     * new myTool.MyFunc(new myTool.MyFunc._Interface() {
     *     @Override
     *     public void _function() {
     *         f();
     *     }
     * }).fn_do();
     * or
     * new myTool.MyFunc(() -> f()).fn_do();
     * or
     * new myTool.MyFunc(this::f()).fn_do();
     */
    static class MyFunc {
        private final _Interface _interface;
        public interface _Interface { void _function();}

        /**
         * 関数を引数とする
         * @param _interface interface: function to do
         */
        public MyFunc(_Interface _interface){this._interface = _interface;}

        /**
         * 関数の実行
         */
        public void fn_do(){
            _interface._function();
        }
    }
}