package com.example.moneycontrol.setting;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moneycontrol.sqliteopenhelper.MoneyTable;
import com.example.moneycontrol.R;
import com.example.moneycontrol.myTool;

public class setting_showall extends AppCompatActivity {
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_all_layout);

        final Handler handler = new Handler(Looper.getMainLooper());

        new Thread(()->{
            db = MoneyTable.newDatabase(this);

            TableLayout table = findViewById(R.id.tableALL);
            TextView tv;

            Cursor c = db.rawQuery(MoneyTable.QUERY_SELECT_ALL(MoneyTable.getTodayTableName()), null);
            c.moveToFirst();

            //LayoutParams
            // https://teratail.com/questions/80290
            int MP = TableRow.LayoutParams.MATCH_PARENT;
            TableRow.LayoutParams LP = new TableRow.LayoutParams(MP, MP);
            LP.setMarginEnd(10);

            for(int i=0; i<c.getCount(); i++){
                TableRow tr = new TableRow(this);

                for(int j=0; j<=5; j++){
                    tv = new TextView(this);
                    String getS = c.getString(j);
                    tv.setText(getS==null?"":getS);

                    try {
                        tv.setLayoutParams(LP);
                        tv.setGravity(Gravity.END);
                    }catch (Exception e){
                        e.printStackTrace();
                    }

                    tr.addView(tv);
                }

                tv = new TextView(this);
                String note;
                String g = myTool.getNullableString(c,6);
                String n = myTool.getNullableString(c,7);
//                空白判定はTextUtils.isEmptyでOK
                if(TextUtils.isEmpty(g) || TextUtils.isEmpty(n)){
                    note = String.format("%s%s", g, n);
                }else{
                    note = String.format("%s : %s", g, n);
                }
                tv.setText(note);
                tr.addView(tv);

                handler.post(()->{
                    table.addView(tr);
                });

                c.moveToNext();
            }
            c.close();

            db.close();
        }).start();
    }
}