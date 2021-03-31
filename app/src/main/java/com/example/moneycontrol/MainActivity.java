package com.example.moneycontrol;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.moneycontrol.setting.SettingsActivity;
import com.example.moneycontrol.sqliteopenhelper.MoneySetting;
import com.example.moneycontrol.sqliteopenhelper.MoneyTable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

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



    private enum IOM {INCOME, OUTGO, MOVE}

    private boolean isMove; //資金移動かどうか

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editMoney = findViewById(R.id.editMoney);
        editMemo = findViewById(R.id.editMemo);
        todayOut = findViewById(R.id.todayOutgoView);
        spnWallet = findViewById(R.id.spinner);
        spnWallet2 = findViewById(R.id.spinner2);

        isMove = false;
        SQLiteDatabase db = MoneyTable.newDatabase(this);
        SQLiteDatabase db_setting = MoneySetting.databaseNullCheck(this, null);

        L_memo = findViewById(R.id.memoLayout);
        L_move = findViewById(R.id.moveLayout);
        L_btn  = findViewById(R.id.ioButtonLayout);
        I_arrow = findViewById(R.id.downArrow);
        //buttonIncome
        Button btn_in = findViewById(R.id.buttonIncome);
        //buttonOutgo
        Button btn_out = findViewById(R.id.buttonOutgo);
        btn_move = findViewById(R.id.moveButton);

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
        editMoney.setOnFocusChangeListener(editFC);
        editMemo.setOnFocusChangeListener(editFC);

        //背景をタッチした時focusを奪う
        findViewById(R.id.backGroundLayout).setOnTouchListener((view, motionEvent) -> {
            view.requestFocus();
            return true;
        });

        //addButtonでeditMoneyする
        FloatingActionButton aB = findViewById(R.id.addButton);
        aB.setOnClickListener(view -> {
            editMoney.requestFocus();
            InputMethodManager imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,InputMethodManager.HIDE_NOT_ALWAYS);
        });

        btn_in.setOnTouchListener(ioButtonFlick);
        btn_out.setOnTouchListener(ioButtonFlick);

        //別スレッドにしたい
        //spinnerにwalletを設定する
        String[] LS = MoneySetting.getList(this, MoneySetting.WALLET);
        spnWallet.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, LS));
        spnWallet2.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, LS));
        setTodaySum();
        readData();
        db.close();
        db_setting.close();
    }
    //End of OnCreate

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
            case MotionEvent.ACTION_DOWN:
                Log.d("down", getIdName(view));
                break;
            case MotionEvent.ACTION_UP:
                Log.d("up", getIdName(view));
                if (isInButton) {
                    Log.d("up", "inButton");
                    iomButton(isIncome ? IOM.INCOME : IOM.OUTGO, null);
                } else {
                    Log.d("up", "outButton");
                    //genre設定してiomButton
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    String[] item = MoneySetting.getList(this, isIncome?0:1);
                    builder.setTitle(R.string.select_genre)
                            .setItems(item, (dialogInterface, i) -> MainActivity.this.iomButton(isIncome ? IOM.INCOME : IOM.OUTGO, item[i])
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
    public void iomButton(IOM iom , @Nullable String genre){
        //genreがnullのとき空文字列にする
        if(genre==null)genre = "";

        Log.i("iomButton", "Button " +
                ( iom == IOM.INCOME ? "income"
                : iom == IOM.OUTGO ? "outgo"
                : /*iom==IOM.MOVE*/ "move")
                + " pushed");
        String money = editMoney.getText().toString();

        //focusを奪う 背景に移す
        findViewById(R.id.backGroundLayout).requestFocus();

        if(!TextUtils.isEmpty(money)){
            SQLiteDatabase db = MoneyTable.newDatabase(this);
            int intMoney = Integer.parseInt(money);

            String text_move = getString(R.string.button_move);

            String wallet = (String) spnWallet.getSelectedItem();
            String wallet2 = (String) spnWallet2.getSelectedItem();
            String note = editMemo.getText().toString();

            //収入支出 資金移動from側の書き込み
            ContentValues cv = new ContentValues();
            if(iom==IOM.INCOME)cv.put("income", money);
            if(iom==IOM.OUTGO)cv.put("outgo", money);
            //MOVEはinout欄には書かない

            int balance = MoneyTable.getBalanceOf(this, wallet);
            if(iom==IOM.INCOME){
                cv.put("balance", balance + intMoney);
            }else{
                cv.put("balance", balance - intMoney);
            }

            cv.put("wallet", wallet);
            cv.put("genre", iom==IOM.MOVE ? text_move : genre.isEmpty() ? "" : genre);
            cv.put("note", iom==IOM.MOVE ? "-"+money : note);
            db.insert(MoneyTable.getTodayTableName(), null, cv);
            Log.d("iomButton", cv.toString());

            //資金移動toの書き込み
            if(iom == IOM.MOVE){
                cv.clear();

                balance = MoneyTable.getBalanceOf(this, wallet2);
                cv.put("balance", balance+intMoney);

                cv.put("wallet", wallet2);
                cv.put("genre", text_move);
                cv.put("note", "+"+money);
                db.insert(MoneyTable.getTodayTableName(), null, cv);
                Log.d("iomButton", cv.toString());
            }
            db.close();
        }
        readData();
        setTodaySum();
        editMoney.setText("");
        editMemo.setText("");
    }


    /**
     * レイアウトを資金移動仕様にする
     */
    private void toMove(){
        if(isMove)return;
        isMove = true;
        L_memo.setVisibility(View.INVISIBLE);
        L_btn.setVisibility(View.INVISIBLE);
        L_move.setVisibility(View.VISIBLE);
        I_arrow.setVisibility(View.VISIBLE);
        btn_move.setText(R.string.cancel);
    }

    /**
     * レイアウトを通常仕様にもどす
     */
    private void fromMove(){
        if(!isMove)return;
        isMove = false;
        L_memo.setVisibility(View.VISIBLE);
        L_btn.setVisibility(View.VISIBLE);
        L_move.setVisibility(View.INVISIBLE);
        I_arrow.setVisibility(View.INVISIBLE);
        btn_move.setText(R.string.button_move);
    }

    /**
     * 資金移動ボタン レイアウトを変えるだけ
     * @param v view
     */
    public void moveButton(View v){
//        Log.d("move", "button pressed");
        if(!isMove){
            toMove();
        }else{
            fromMove();
        }
    }

    /**
     * 資金移動実行ボタン 書き換え処理はここから
     * @param v view
     */
    public void moveDoButton(View v){
//        Log.d("moveDo", "button pressed");

        if(isMove){
            //正常処理
//            Log.d("movedo", "do Move");
            iomButton(IOM.MOVE, null);

            fromMove();
        }
    }

    /**
     * 直近の項目を削除する
     * @param v view
     */
    public void undoButton(View v){
//        Log.d("undoButton", "clicked");
        SQLiteDatabase sqLiteDatabase = MoneyTable.newDatabase(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        StringBuilder lastItem = new StringBuilder("|");
        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM MoneyDatabase ORDER BY _id DESC LIMIT 3", null);
        cursor.moveToFirst();
        //削除するものが無い
        if(cursor.getCount()==0){
            sqLiteDatabase.close();
//            Log.d("undoButton", "nothing to delete");
            builder.setTitle("直近項目削除")
                    .setMessage("削除できる項目がありません")
                    .setPositiveButton("OK",null)
                    .show();
            return;
        }
        //直近が資金移動だったら2つ進める
//        if(cursor.getString(6).equals(getString(R.string.button_move))){
//            cursor.moveToNext();
//            cursor.moveToNext();
//        }
        int id = cursor.getInt(0);
        for(int i=1; i<=6; i++){
            lastItem.append(cursor.getString(i)).append("|");
        }
        cursor.close();
        builder.setTitle("直近項目削除")
                .setMessage(lastItem.toString())
                .setPositiveButton("削除", (dialogInterface, i) -> {
                    Log.d("delete", ""+id+" delete");
                    final SQLiteDatabase sqlDB = MoneyTable.newDatabase(this);
                    sqlDB.execSQL("DELETE FROM "+ MoneyTable.getTodayTableName()+" WHERE _id="+id);
                    sqlDB.close();
                    readData();
                    setTodaySum();
                })
                .setNeutralButton("キャンセル", (dialogInterface, i) -> {
                    //nothing
                });
        builder.show();
        sqLiteDatabase.close();
    }

    /**
     * 最新最大5件の読み取り
     */
    private void readData(){
        SQLiteDatabase db = MoneyTable.newDatabase(this);
        Cursor cursor = MoneyTable.getNewData(db, 5);

        //読み取り
        cursor.moveToFirst();
        TextView tv;

        int i;
        for(i=0; i<cursor.getCount(); i++){
            //sb.append(cursor.getInt(0)); sb.append(" "); //最初は_idなので読まない

            String timestamp = cursor.getString(1).substring(5, 16);
            int getIncome = cursor.getInt(2), getOutgo = cursor.getInt(3);
            String status;
            if(getIncome+getOutgo==0) status = "";
            else if(getIncome > 0) status = getString(R.string.status_income);
            else status = getString(R.string.status_outgo);

            String money = cursor.getInt(2)>0 ? cursor.getString(2): cursor.getString(3);
            String wallet = cursor.getString(5);
            String note;
            String g = cursor.getString(6);
            String n = cursor.getString(7);
//            TODO: 要る
            if(g==null)g="";
            if(n==null)n="";
            if(g.isEmpty() || n.isEmpty()){
                note = String.format("%s%s", g, n);
            }else{
                note = String.format("%s : %s", g, n);
            }

            setHistoryTable(i, new String[]{timestamp, status, money, wallet, note});

            cursor.moveToNext();
        }
        for(; i<5; i++)setHistoryTable(i, null);

        cursor.close();
        db.close();
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

    private void setTodaySum(){todayOut.setText(String.format(Locale.US, "%d", MoneyTable.todaySum(this)));}

    public void settingButton(View v){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}