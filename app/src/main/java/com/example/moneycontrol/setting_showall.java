package com.example.moneycontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

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

        for(int i=0; i<c.getCount(); i++){
            TableRow tr = new TableRow(this);

            for(int j=1; j<=5; j++){
                tv = new TextView(this);
                tv.setText(c.getString(j));
                /*
                try {
                    int WC = ViewGroup.LayoutParams.WRAP_CONTENT;
                    ViewGroup.LayoutParams LP = new ViewGroup.LayoutParams(WC, WC);
                    ViewGroup.MarginLayoutParams MLP = new ViewGroup.MarginLayoutParams(WC, WC);
                    MLP.setMargins(0, 0, 2, 0);
                    tv.setLayoutParams(MLP);
                }catch (Exception e){
                    Log.d("setMarginError", e.toString());
                }
                //*/

                try{
                    tv.setGravity(Gravity.END);
                }catch (Exception e){
                    Log.d("setGravityError", e.toString());
                }
                tr.addView(tv);
            }

            tv = new TextView(this);
            String strNote = (c.getString(6).isEmpty() ? "" : c.getString(6) + " : ") + c.getString(7);
            tv.setText(strNote);
            tr.addView(tv);

            table.addView(tr);

            c.moveToNext();
        }
        c.close();
    }
}