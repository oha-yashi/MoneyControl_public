package com.example.moneycontrol.setting;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.moneycontrol.sqliteopenhelper.MoneyTable;
import com.example.moneycontrol.R;
import com.example.moneycontrol.myTool;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class setting_showall extends AppCompatActivity {
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_all_layout);

        handler = new Handler(Looper.getMainLooper());

        showAll(MoneyTable.getTodayTableName());
    }

    private void toastError(){Toast.makeText(this,"データが存在しません",Toast.LENGTH_SHORT).show();}

    private void showAll(String table_name){
        if(!MoneyTable.getMoneyTableNames(this).contains(MoneyTable.getTodayTableName())){
            toastError();
            return;
        }

        new Thread(()->{
            try(SQLiteDatabase db = MoneyTable.newDatabase(this)) {
                TableLayout table = findViewById(R.id.tableALL);
                TextView tv;

                Cursor c = db.rawQuery(MoneyTable.QUERY_SELECT_ALL(table_name), null);
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
                        tv.setText(myTool.nullToSpace(getS));

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
            }
        }).start();
    }

    //    メニュー設定
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.showallmenu, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.show_all_select:{
                Spinner spinner = new Spinner(this);
                List<String> l = MoneyTable.getMoneyTableNames(this);
//                Collections.sort(l, new Comparator<String>() {
//                    @Override
//                    public int compare(String s, String t1) {
//                        return s.compareTo(t1);
//                    }
//                });

                l.sort(Collections.reverseOrder());

//                l.sort(String::compareTo);
                spinner.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, l));

                new AlertDialog.Builder(this)
                        .setTitle("表示テーブル設定")
                        .setView(spinner)
                        .setPositiveButton("表示",(dialogInterface, i) -> {
                            try {
                                showAll(((String) spinner.getSelectedItem()));
                            } catch(Exception e){
                                toastError();
                                e.printStackTrace();
                            }
                        }).show();
            }
            default : return super.onOptionsItemSelected(item);
        }
    }
}