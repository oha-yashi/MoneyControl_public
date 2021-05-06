package com.example.moneycontrol.dbTools;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.moneycontrol.MyTool;
import com.example.moneycontrol.R;
import com.example.moneycontrol.sqliteopenhelper.MoneySetting;
import com.example.moneycontrol.sqliteopenhelper.MoneyTable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EditDB extends AppCompatActivity {
    Handler handler;
    String table;
    int id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        透明Activity
        handler = new Handler(Looper.getMainLooper());
        table = MoneyTable.getTodayTableName();
        id = MoneyTable.getRecentId(this);

        start();
    }

//    @Override
//    protected void onResume() {
//        super.onResume();
//        start();
//    }

    private void start(){
        new Thread(()-> {
            View v = getLayoutInflater().inflate(R.layout.edit_db_gettableid, null);

            Spinner spinner = v.findViewById(R.id.editDB_spinner);
            List<String> l = MoneyTable.getMoneyTableNames(this);
            l.sort(Comparator.reverseOrder());
            spinner.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, l));

            EditText editText = v.findViewById(R.id.editDB_editText);
//        editText.setText(String.valueOf(id)); // 最初にselectedが呼ばれて自動的にsetされる

            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
//                new Thread(()->{
//                    id = MoneyTable.getRecentId(adapterView.getContext(),adapterView.getSelectedItem().toString());
//                    handler.post(()-> editText.setText(String.valueOf(id)));
//                }).start();
                    id = MoneyTable.getRecentId(adapterView.getContext(), adapterView.getSelectedItem().toString());
                    editText.setText(String.valueOf(id));
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {

                }
            });
            handler.post(()->new AlertDialog.Builder(this).setTitle("TABLE,ID選択").setView(v)
                    .setPositiveButton("検索", (dialogInterface, i) -> {
                        table = spinner.getSelectedItem().toString();
                        id = Integer.parseInt(editText.getText().toString());
                        MyTool.MyLog.format("selected table=%s, id=%d", table, id);
//                    dialogInterface.dismiss();

                        if (MoneyTable.isExist(this, table, id)) {
                            EnableList enableList = new EnableList().setNoteEnable(true).setTimestampEnable(true);
                            editDialog(enableList);
                        } else {
                            Toast.makeText(this, "存在しないidです", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
//                .setOnDismissListener(dialogInterface -> {
//                    MyTool.MyLog.d("dismissed");
//                    // ここでfinish()してしまうと、正常遷移の時にもActivityがfinishされてwindowLeakになる
//                })
                    .setOnCancelListener(dialogInterface -> finish()) //画面外タッチで終了
                    .show());
        }).start();
    }

    private void editDialog(EnableList e){
        new Thread(()->{
            View v = getLayoutInflater().inflate(R.layout.edit_db,null);

            EditText _editId = v.findViewById(R.id.no_edit_id);
            EditText editTimestamp = v.findViewById(R.id.edit_timestamp);
            EditText editIncome = v.findViewById(R.id.edit_income);
            EditText editOutgo = v.findViewById(R.id.edit_outgo);
            EditText editBalance = v.findViewById(R.id.edit_balance);
            Spinner editWallet = v.findViewById(R.id.edit_wallet);
            Spinner editGenre = v.findViewById(R.id.edit_genre);
            EditText editNote = v.findViewById(R.id.edit_note);

            editTimestamp.setEnabled(e.timestampEnable);
            editIncome.setEnabled(e.incomeEnable);
            editOutgo.setEnabled(e.outgoEnable);
            editBalance.setEnabled(e.balanceEnable);
            editWallet.setEnabled(e.walletEnable);
            editGenre.setEnabled(e.genreEnable);
            editNote.setEnabled(e.noteEnable);

            List<String>  walletList = MoneySetting.getList(this,2).second;
            editWallet.setAdapter(new ArrayAdapter<>(this,R.layout.support_simple_spinner_dropdown_item,walletList));
            List<String> genreList = MoneySetting.getGenreList(this);
            genreList.add("資金移動");
            editGenre.setAdapter(new ArrayAdapter<>(this,R.layout.support_simple_spinner_dropdown_item,genreList));

            try(SQLiteDatabase db = MoneyTable.newDatabase(this)){
                Cursor cursor = db.rawQuery(MoneyTable.QUERY_SELECT_BY_ID(table,id),null);
                cursor.moveToFirst();
                _editId.setText(cursor.getString(0));
                editTimestamp.setText(cursor.getString(1));
                editIncome.setText(cursor.getString(2));
                editOutgo.setText(cursor.getString(3));
                editBalance.setText(cursor.getString(4));
                editWallet.setSelection(walletList.indexOf(cursor.getString(5)));
                editGenre.setSelection(genreList.indexOf(cursor.getString(6)));
                editNote.setText(cursor.getString(7));
            }

            handler.post(()->new AlertDialog.Builder(this).setTitle(table + " : " + id).setView(v)
                    .setPositiveButton("保存",(dialogInterface, i) -> {
                        List<Pair<String,String>> changeList = new ArrayList<>();

                        if(e.timestampEnable){
                            String newTimestamp = editTimestamp.getText().toString();
                            try {
                                SimpleDateFormat sdf = MyTool.timestampFormat;
                                sdf.setLenient(false);
                                sdf.parse(newTimestamp);
                                // timestampFormatに合致していなければここでcatchに飛ぶ
                                changeList.add(Pair.create("timestamp",newTimestamp));
                            } catch (ParseException parseException) {
//                                Toast.makeText(this,"timastamp parse Error",Toast.LENGTH_SHORT).show();
                                parseException.printStackTrace();
                            }
                        }
                        if(e.noteEnable)changeList.add(Pair.create("note",editNote.getText().toString()));

                        editDatabase(changeList);
                    })
                    .setNeutralButton("閉じる",(dialogInterface, i) -> finish())
                    .setOnCancelListener(dialogInterface -> finish())
                    .show());

        }).start();
    }

    private void editDatabase(List<Pair<String,String>> changeList){editDatabase(table,id,changeList);}
    private void editDatabase(String _table,int _id,List<Pair<String,String>> changeList){
        StringBuilder q = new StringBuilder("UPDATE "+_table+" SET");
        for(Pair<String,String> p:changeList){
            q.append(" "+p.first+"='"+p.second+"',");
        }
        q.deleteCharAt(q.lastIndexOf(","));
        q.append(" WHERE _id="+_id);

        MyTool.MyLog.d(q.toString());

        try(SQLiteDatabase db = MoneyTable.newDatabase(this)){
            db.execSQL(q.toString());
            Toast.makeText(this,"成功！",Toast.LENGTH_SHORT).show();
        } catch (Exception e){
            Toast.makeText(this,"失敗",Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private class EnableList {
        public boolean timestampEnable;
        public boolean incomeEnable;
        public boolean outgoEnable;
        public boolean balanceEnable;
        public boolean walletEnable;
        public boolean genreEnable;
        public boolean noteEnable;

        EnableList(boolean t, boolean i, boolean o, boolean b, boolean w, boolean g, boolean n){
            this.timestampEnable = t;
            this.incomeEnable = i;
            this.outgoEnable = o;
            this.balanceEnable = b;
            this.walletEnable = w;
            this.genreEnable = g;
            this.noteEnable = n;
        }

        EnableList(){
            new EnableList(false,false,false,false,false,false,false);
        }

        public EnableList setTimestampEnable(boolean timestampEnable) {
            this.timestampEnable = timestampEnable;
            return this;
        }

        public EnableList setNoteEnable(boolean noteEnable) {
            this.noteEnable = noteEnable;
            return this;
        }
    }
}