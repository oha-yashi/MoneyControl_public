package com.example.moneycontrol;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class setting_showall extends AppCompatActivity {
    private SQLiteDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_all_layout);
        db = MoneyTableOpenHelper.databaseNullCheck(this, db);
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

            for(int j=1; j<5; j++){
                tv = new TextView(this);
                tv.setText(c.getString(j));
                tr.addView(tv);
            }

            tv = new TextView(this);
            String strNote = (c.getString(5).isEmpty() ? "" : c.getString(5) + " : ") + c.getString(6);
            tv.setText(strNote);
            tr.addView(tv);

            table.addView(tr);

            c.moveToNext();
        }
        c.close();
    }
}