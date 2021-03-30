package com.example.moneycontrol.setting;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moneycontrol.MoneyTableOpenHelper;
import com.example.moneycontrol.R;

public class setting_showall extends AppCompatActivity {
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_all_layout);
        db = MoneyTableOpenHelper.newDatabase(this);
        showAll();
        db.close();
    }

    public void showAll(){
        TableLayout table = findViewById(R.id.tableALL);
        TextView tv;

        Cursor c = db.rawQuery(MoneyTableOpenHelper.READ_ALL_QUERY, null);
        c.moveToFirst();

        //LayoutParams
        // https://teratail.com/questions/80290
        int WC = TableRow.LayoutParams.MATCH_PARENT;
        TableRow.LayoutParams LP = new TableRow.LayoutParams(WC, WC);
        LP.setMarginEnd(10);

        for(int i=0; i<c.getCount(); i++){
            TableRow tr = new TableRow(this);

            for(int j=1; j<=5; j++){
                tv = new TextView(this);
                String getS = c.getString(j);
                tv.setText(getS==null?"":getS);

                try {
                    tv.setLayoutParams(LP);
                }catch (Exception e){
                    Log.d("setMarginError", e.toString());
                }

                try{
                    tv.setGravity(Gravity.END);
                }catch (Exception e){
                    Log.d("setGravityError", e.toString());
                }
                tr.addView(tv);
            }

            tv = new TextView(this);
            String note;
            String g = c.getString(6);
            String n = c.getString(7);
//            TODO: 要る
            if(g==null)g="";
            if(n==null)n="";
            if(g.isEmpty() || n.isEmpty()){
                note = String.format("%s%s", g, n);
            }else{
                note = String.format("%s : %s", g, n);
            }
            tv.setText(note);
            tr.addView(tv);

            table.addView(tr);

            c.moveToNext();
        }
        c.close();
    }
}