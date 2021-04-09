package com.example.moneycontrol;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.moneycontrol.setting.SettingsActivity;
import com.example.moneycontrol.sqliteopenhelper.MoneySetting;
import com.example.moneycontrol.sqliteopenhelper.MoneyTable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    private EditText editMoney;
    private EditText editMemo;
    private TextView todayOut;
    private Spinner spnWallet;
    private Spinner spnWallet2;
    private ConstraintLayout L_memo;
    private ConstraintLayout L_move;
    private ConstraintLayout L_btn;
    private ImageView I_arrow;
    private Button btn_move; //buttonMove

    private Handler handler;

//    private enum IOM {INCOME, OUTGO, MOVE}
    private static final int IOM_INCOME = 1;
    private static final int IOM_OUTGO = 2;
    private static final int IOM_MOVE = 3;

    private boolean isMove; //資金移動かどうか

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler(Looper.getMainLooper());

        editMoney = findViewById(R.id.editMoney);
        editMemo = findViewById(R.id.editMemo);
        todayOut = findViewById(R.id.todayOutgoView);
        spnWallet = findViewById(R.id.spinner);
        spnWallet2 = findViewById(R.id.spinner2);
        btn_move = findViewById(R.id.moveButton);

        isMove = false;

        /**
         * editText用focusChangeListener
         * フォーカスを得たらキーボードを出す。フォーカスが無くなったら隠す
         */
        View.OnFocusChangeListener editFC = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean isFocused) {
                InputMethodManager inputMethodManager = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                if(isFocused){
                    inputMethodManager.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, InputMethodManager.HIDE_NOT_ALWAYS);
                }else{
                    inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                }
            }
        };

        new Thread(()->{
            L_memo = findViewById(R.id.memoLayout);
            L_move = findViewById(R.id.moveLayout);
            L_btn  = findViewById(R.id.ioButtonLayout);
            I_arrow = findViewById(R.id.downArrow);
            ((Button) findViewById(R.id.buttonIncome)).setOnTouchListener(ioButtonFlick);
            ((Button) findViewById(R.id.buttonOutgo)).setOnTouchListener(ioButtonFlick);
            btn_move.setOnClickListener(view -> {
                if(!isMove){
                    isMove = true;
                    L_memo.setVisibility(View.INVISIBLE);
                    L_btn.setVisibility(View.INVISIBLE);
                    L_move.setVisibility(View.VISIBLE);
                    I_arrow.setVisibility(View.VISIBLE);
                    btn_move.setText(R.string.cancel);
                }else{
                    isMove = false;
                    L_memo.setVisibility(View.VISIBLE);
                    L_btn.setVisibility(View.VISIBLE);
                    L_move.setVisibility(View.INVISIBLE);
                    I_arrow.setVisibility(View.INVISIBLE);
                    btn_move.setText(R.string.button_move);
                }
            });
            ((Button) findViewById(R.id.moveDoButton)).setOnClickListener(view -> {
                if(isMove){
                    //正常処理
                    iomButton(IOM_MOVE, null);
                    isMove = false;
                    L_memo.setVisibility(View.VISIBLE);
                    L_btn.setVisibility(View.VISIBLE);
                    L_move.setVisibility(View.INVISIBLE);
                    I_arrow.setVisibility(View.INVISIBLE);
                    btn_move.setText(R.string.button_move);
                }
            });
            editMoney.setOnFocusChangeListener(editFC);
            editMemo.setOnFocusChangeListener(editFC);

            //spinnerにwalletを設定する
            String[] LS = MoneySetting.getList(this, MoneySetting.WALLET);
//            handler.post(()->{
                spnWallet.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, LS));
                spnWallet2.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, LS));
//            });

            //背景をタッチした時focusを奪う
            findViewById(R.id.backGroundLayout).setOnTouchListener((view, motionEvent) -> {
                view.requestFocus();
                return true;
            });

            //addButtonでeditMoneyにフォーカス当てる
            ((FloatingActionButton) findViewById(R.id.addButton)).setOnClickListener(view ->
                    editMoney.requestFocus());
            Log.d("timing", "end of thread");
        }).start();

//        functionButtonの設定
        String[] functions = new String[]{"残額表示", "id削除"};
        ((FloatingActionButton) findViewById(R.id.functionButton)).setOnClickListener(view -> {
            new AlertDialog.Builder(this).setTitle("多機能ボタン")
                    .setItems(functions, (dialogInterface, i) -> {
                        switch(i){
                            case 0:
                                fn_checkBalanceDialog(); break;
                            case 1:{
                                fn_deleteById(); break;
                            }
                        }
                    })
                    .setNeutralButton("閉じる",null)
                    .show();
        });
        Log.d("timing", "before reload");
        reload();
    }
    //End of OnCreate


    @Override
    protected void onRestart() {
        super.onRestart();
        reload();
    }

    //    メニュー設定
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mymenu, menu);
        return true;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.menu_to_setting:{
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            }
            case R.id.menu_reload:{
                for(int i=0; i<5; i++)setHistoryTable(i,null);
                reload();
            }
            default : return super.onOptionsItemSelected(item);
        }
    }


    //inoutボタンのフリック設定
    @SuppressLint("ClickableViewAccessibility")
    private final View.OnTouchListener ioButtonFlick = (view, motionEvent) -> {
        Button btn = (Button) view;
        float gX = motionEvent.getX();
        float gY = motionEvent.getY();
        int buttonWidth = view.getWidth();
        int buttonHeight = view.getHeight();
        boolean isInButton = 0 <= gX && gX <= buttonWidth && 0 <= gY && gY <= buttonHeight;
        boolean isIncome = getIdName(view).equals("buttonIncome");

        int action = motionEvent.getAction();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (isInButton /* && view instanceof Button */) {
                    btn.setText(isIncome ? R.string.button_income : R.string.button_outgo);
                } else {
                    btn.setText(R.string.select_genre);
                }
                break;
            case MotionEvent.ACTION_UP:
                if (isInButton) {
                    iomButton(isIncome ? IOM_INCOME : IOM_OUTGO, null);
                } else {
                    //genre設定してiomButton
                    String[] item = MoneySetting.getList(this, isIncome?0:1);
                    new AlertDialog.Builder(this).setTitle(R.string.select_genre)
                            .setItems(item, (dialogInterface, i) ->
                                    MainActivity.this.iomButton(isIncome ? IOM_INCOME : IOM_OUTGO, item[i])
                            ).show();
                }
                btn.setText(isIncome ? R.string.button_income : R.string.button_outgo);
                break;
        }
        return false;
    };

    private String getIdName(@NotNull View v){
        return getResources().getResourceName(v.getId()).split(":id/")[1];
    }

    /**
     * 支出収入資金移動ボタン
     * @param iom enum IOM
     * @param genre nullable
     */
    public void iomButton(int iom , @Nullable String genre){
        String money = editMoney.getText().toString();

        //focusを奪う 背景に移す
        findViewById(R.id.backGroundLayout).requestFocus();

        new Thread(()->{
            if(!TextUtils.isEmpty(money)){
                int intMoney = Integer.parseInt(money);
                String text_move = getString(R.string.button_move);
                String wallet = (String) spnWallet.getSelectedItem();
                String wallet2 = (String) spnWallet2.getSelectedItem();
                String note = editMemo.getText().toString();

                int balance = MoneyTable.getBalanceOf(this, wallet);
                switch(iom){
                    case IOM_INCOME:{
                        MoneyTable.insert(this, null, intMoney, null,
                                balance+intMoney, wallet, genre, note);
                        break;
                    }
                    case IOM_OUTGO:{
                        MoneyTable.insert(this,null,null, intMoney,
                                balance-intMoney, wallet, genre, note);
                        break;
                    }
                    case IOM_MOVE:{
                        if(wallet.equals(wallet2)){
                            handler.post(()->{
                                String message = "同walletへの資金移動は不可!!";
//                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                                Snackbar.make(findViewById(R.id.backGroundLayout), message, Snackbar.LENGTH_LONG).show();
                            });
                            break;
                        }
    //                    資金移動fromの書き込み
                        MoneyTable.insert(this, null, null, null,
                                balance-intMoney, wallet, text_move, "-"+money);
    //                    資金移動toの書き込み
                        MoneyTable.insert(this, null, null, null,
                                balance+intMoney, wallet2, text_move, "+"+money);
                        break;
                    }
                }

            }
            handler.post(()-> reload());
        }).start();
    }

    /**
     * 最新最大5件の読み取り
     * done in thread
     */
    private void readData(){
//        https://qiita.com/8yabusa/items/f8c9bb7eb81175c49e97
        new Thread(() -> {

            SQLiteDatabase db = MoneyTable.newDatabase(this);
            Cursor cursor = MoneyTable.getNewTimeData(db, 5);

            //読み取り
            cursor.moveToFirst();

            int i;
            for (i = 0; i < cursor.getCount(); i++) {
                //sb.append(cursor.getInt(0)); sb.append(" "); //最初は_idなので読まない

                String timestamp = cursor.getString(1).substring(5, 16);
                int getIncome = cursor.getInt(2), getOutgo = cursor.getInt(3);
                String status;
                if (getIncome + getOutgo == 0) status = "";
                else if (getIncome > 0) status = getString(R.string.status_income);
                else status = getString(R.string.status_outgo);

                String money = cursor.getInt(2) > 0 ? cursor.getString(2) : cursor.getString(3);
                String wallet = cursor.getString(5);
                String note;
                String g = myTool.getNullableString(cursor, 6);
                String n = myTool.getNullableString(cursor, 7);
                if (TextUtils.isEmpty(g) || TextUtils.isEmpty(n)) {
                    note = String.format("%s%s", g, n);
                } else {
                    note = String.format("%s : %s", g, n);
                }

                int ii = i;
                handler.post(()-> setHistoryTable(ii, new String[]{timestamp, status, money, wallet, note}));
                cursor.moveToNext();
            }
            for (; i < 5; i++) {
                int ii = i;
                handler.post(()->setHistoryTable(ii, null));
            }

            cursor.close();
            db.close();
        }).start();
    }

    /**
     * メイン画面の履歴表示に書き込む
     * @param i 上からの列数 0-index
     * @param items String[] {timestamp, status, money, wallet, note(genre+memo)}
     */
    private void setHistoryTable(int i, @Nullable String[] items){
        String noneTime = getString(R.string.table_none_time);
        String none = getString(R.string.table_none);
        TextView tv;

        if(items!=null && items.length==5){
            tv = findViewById(getResources().getIdentifier("tableDate"+i, "id", getPackageName()));
            tv.setText(items[0]);
            tv = findViewById(getResources().getIdentifier("tableStatus"+i, "id", getPackageName()));
            tv.setText(items[1]);
            tv = findViewById(getResources().getIdentifier("tableMoney"+i, "id", getPackageName()));
            tv.setText(items[2]);
            tv = findViewById(getResources().getIdentifier("tableWallet"+i, "id", getPackageName()));
            tv.setText(items[3]);
            tv = findViewById(getResources().getIdentifier("tableMemo"+i, "id", getPackageName()));
            tv.setText(items[4]);
        }else{
            setHistoryTable(i, new String[] {noneTime, none, none, none, none});
        }
    }

    private void setTodaySum(){
        todayOut.setText(String.format(Locale.US, "%d (%d)",
            MoneyTable.todaySum(this), MoneyTable.monthAverage(this)));
    }

    public void reload(){
        Log.d("reload", "start");
        editMoney.getText().clear();
        editMemo.getText().clear();
        readData();
        setTodaySum();
    }

//    functionButtonに入れる機能
    private void fn_checkBalanceDialog(){
        CharSequence[] walletList = MoneySetting.getList(this, MoneySetting.WALLET);
        new AlertDialog.Builder(this).setTitle("残額表示")
                .setSingleChoiceItems(walletList, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new Thread(()->{
                            String balance = MoneyTable.getBalanceOf(MainActivity.this, walletList[i].toString()) + "円";
                            handler.post(()-> Toast.makeText(MainActivity.this, balance, Toast.LENGTH_SHORT).show());
                        }).start();
                    }
                }).setNeutralButton("閉じる", null).show();
    }
    private void fn_deleteById(){
        final EditText editId = new EditText(this);
        editId.setText(String.valueOf(MoneyTable.getRecentId(this)));
        editId.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle("id削除").setMessage("直近のID入力ずみ")
            .setView(editId)
            .setPositiveButton("OK", (dialogInterface, i) -> {
                String getId = editId.getText().toString();
                if(getId==null || getId.isEmpty())return;
                int intId = Integer.parseInt(getId);

                fn_deleteById_confirmDialog(intId);

            }).show();
    }

    private void fn_deleteById_confirmDialog(int id){
        try {
            String message = MoneyTable.toStringTableId(this, MoneyTable.getTodayTableName(), id);

            new AlertDialog.Builder(this).setTitle("削除確認")
                    .setMessage(message)
                    .setPositiveButton("削除", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            new Thread(()->{
                                MoneyTable.deleteById(MainActivity.this, id);
                                Log.d("checkTiming", "start reload");
                                handler.post(()->{
                                    readData();
                                    setTodaySum();
                                });
                            }).start();
                        }
                    }).setNeutralButton("削除しない", null).show();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }


    }
}