package com.example.moneycontrol;

import android.database.Cursor;

/**
 * 各class固有でもない、使える関数とかを置いておく
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
}
