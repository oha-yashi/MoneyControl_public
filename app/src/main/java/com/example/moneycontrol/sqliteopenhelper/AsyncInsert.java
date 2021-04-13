package com.example.moneycontrol.sqliteopenhelper;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

@SuppressLint("StaticFieldLeak")
public class AsyncInsert extends AsyncTask<InsertParams, Object, Boolean> {
    private final Listener listener;
    public interface Listener {
        void afterInsert();
    }
    private final Context context;

    /**
     * コンストラクタ
     * @param context this
     * @param listener this::reload
     */
    public AsyncInsert(Context context, Listener listener) {
        this.listener = listener;
        this.context = context;
        Log.d("AsyncInsert", "make new");
    }

    @Override
    protected Boolean doInBackground(InsertParams... insertParams) {
        for(InsertParams p: insertParams){
            String calendarTableName = MoneyTable.getCalendarTableName(p.calendar);
            try (SQLiteDatabase db = MoneyTable.newDatabase(context)){
                db.execSQL(MoneyTable.QUERY_CREATE(calendarTableName));
                db.insert(calendarTableName, null, p.toContentValues());
                Log.d("timing", "MoneyTable.insert : done");
            }catch (Exception e){
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean isSuccess){
        if(listener != null && isSuccess){
            listener.afterInsert();
        }
    }

}
