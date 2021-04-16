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
import android.util.Pair;
import android.view.Gravity;
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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;

import com.example.moneycontrol.setting.SettingsActivity;
import com.example.moneycontrol.sqliteopenhelper.AsyncInsert;
import com.example.moneycontrol.sqliteopenhelper.InsertParams;
import com.example.moneycontrol.sqliteopenhelper.MoneySetting;
import com.example.moneycontrol.sqliteopenhelper.MoneyTable;
import com.google.android.material.snackbar.Snackbar;

import java.time.YearMonth;
import java.util.Calendar;
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

        new Thread(()->{
            L_memo = findViewById(R.id.memoLayout);
            L_move = findViewById(R.id.moveLayout);
            L_btn  = findViewById(R.id.ioButtonLayout);
            I_arrow = findViewById(R.id.downArrow);
            findViewById(R.id.buttonIncome).setOnTouchListener(ioButtonFlick);
            findViewById(R.id.buttonOutgo).setOnTouchListener(ioButtonFlick);
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
            findViewById(R.id.moveDoButton).setOnClickListener(view -> {
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
            findViewById(R.id.addButton).setOnClickListener(view ->
                    editMoney.requestFocus());
            Log.d("timing", "end of thread");
        }).start();

//        functionButtonの設定
        String[] functions = new String[]{"残額表示", "id削除"};
        findViewById(R.id.functionButton).setOnClickListener(view -> {
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
                reload();
                return true;
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

    private String getIdName(@NonNull View v){
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

        if(!TextUtils.isEmpty(money)){
            int intMoney = Integer.parseInt(money);
            String text_move = getString(R.string.button_move);
            String wallet = (String) spnWallet.getSelectedItem();
            String wallet2 = (String) spnWallet2.getSelectedItem();
            String note = editMemo.getText().toString();

            int balance = MoneyTable.getBalanceOf(this, wallet);
            AsyncInsert asyncInsert = new AsyncInsert(this, ()->reload(false) );
            switch(iom){
                case IOM_INCOME:{
                    asyncInsert.execute(new InsertParams(
                            null, intMoney, null,
                            balance+intMoney, wallet, genre, note));
                    break;
                }
                case IOM_OUTGO:{
                    asyncInsert.execute(new InsertParams(
                            null,null,intMoney,
                            balance-intMoney, wallet,genre,note));
                    break;
                }
                case IOM_MOVE:{
                    if(wallet.equals(wallet2)){
                        String message = "同walletへの資金移動は不可!!";
//                                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                        Snackbar.make(findViewById(R.id.myCoordinatorLayout), message, Snackbar.LENGTH_LONG).show();
                        break;
                    }
//                    資金移動fromの書き込み
                    InsertParams moveFromParams = new InsertParams(
                            null,null,null,
                            balance-intMoney,wallet,text_move,"-"+money);
//                    資金移動toの書き込み
                    balance = MoneyTable.getBalanceOf(this,wallet2);
                    InsertParams moveToParams = new InsertParams(null, null, null,
                            balance+intMoney,
                            wallet2, text_move, "+"+money);
                    asyncInsert.execute(moveFromParams, moveToParams);
                    break;
                }
            }
        }
    }

    /**
     * 最新最大件の読み取り
     * done in thread
     * @param limit
     */
    private void readData(int limit){
//        https://qiita.com/8yabusa/items/f8c9bb7eb81175c49e97

        if(limit<5)limit=5;

        int finalLimit = limit;
        new Thread(() -> {
            final View tableLayout = this.getLayoutInflater().inflate(R.layout.main_table, null);
            final LinearLayout wrapper = findViewById(R.id.tableWrapper);
            handler.post(() -> {
                wrapper.removeAllViews();
                wrapper.addView(tableLayout);
            });
            try(SQLiteDatabase db = MoneyTable.newDatabase(this)) {
                Cursor cursor = MoneyTable.getNewTimeData(db, finalLimit);

                //読み取り
                cursor.moveToFirst();

                int i;
                for (i = 0; i < cursor.getCount(); i++) {
                    setHistoryTable((TableLayout)tableLayout, new InsertParams(cursor));
                    cursor.moveToNext();
                }

                cursor.close();
            }
            Log.d("readData", "finished");
        }).start();
    }

    /**
     * メイン画面の履歴表示に書き込む。順に下につながる。Thread内使用を想定
     * @param params 書き込むinsertパラメータ
     */
    private void setHistoryTable(TableLayout tableLayout, InsertParams params){
        String textStatus =
                params.income > 0 ? getString(R.string.status_income) :
                params.outgo > 0 ? getString(R.string.status_outgo) : "";
        String textMoney =
                params.income > 0 ? params.income.toString() :
                params.outgo > 0 ? params.outgo.toString() : "";
        String g = params.genre;
        String n = params.note;
        String textNote;
        if (TextUtils.isEmpty(g) || TextUtils.isEmpty(n)) {
            textNote = String.format("%s%s", g, n);
        } else {
            textNote = String.format("%s : %s", g, n);
        }
        Pair<?,?>[] set_TextGravity = new Pair[]{
                Pair.create(myTool.toTimestamp(params.calendar),Gravity.START),
                Pair.create(textStatus, Gravity.CENTER),
                Pair.create(textMoney, Gravity.END),
                Pair.create(params.wallet, Gravity.CENTER),
                Pair.create(textNote, Gravity.START),
                Pair.create(params.balance.toString(), Gravity.END)
        };
        TableRow tr = new TableRow(this);
        TextView tv;

        for (Pair<?, ?> p: set_TextGravity) {
            tv = new TextView(this);
            tv.setPadding(3, 3, 3, 3);
            tv.setText((String) p.first);
            tv.setGravity((Integer) p.second);
            tr.addView(tv);
        }

////            timestamp
//        tv = new TextView(this);
//        tv.setPadding(3, 3, 3, 3);
//        tv.setText(myTool.toTimestamp(params.calendar));
//        tr.addView(tv);
////            status
//        tv = new TextView(this);
//        tv.setPadding(3, 3, 3, 3);
//        if(params.income > 0){
//            tv.setText(R.string.status_income);
//        }else if(params.outgo > 0){
//            tv.setText(R.string.status_outgo);
//        }
//        tr.addView(tv);
////            money
//        tv = new TextView(this);
//        tv.setPadding(3, 3, 3, 3);
//        tv.setGravity(Gravity.END);
//        if(params.income > 0){
//            tv.setText(String.valueOf(params.income));
//        }else if(params.outgo > 0){
//            tv.setText(String.valueOf(params.outgo));
//        }
//        tr.addView(tv);
////            wallet
//        tv = new TextView(this);
//        tv.setPadding(3, 3, 3, 3);
//        tv.setGravity(Gravity.CENTER);
//        tv.setText(params.wallet);
//        tr.addView(tv);
////            textNote
//        tv = new TextView(this);
//        tv.setPadding(3, 3, 3, 3);
//        tv.setText(textNote);
//        tr.addView(tv);
////            balance
//        tv = new TextView(this);
//        tv.setPadding(3, 3, 3, 3);
//        tv.setGravity(Gravity.END);
//        tv.setText(String.valueOf(params.balance));
//        tr.addView(tv);

        handler.post(()->tableLayout.addView(tr));
    }

    private void setTodaySum(){
        int t_sum = MoneyTable.todaySum(this);
        int m_sum = MoneyTable.monthSum(this);
        int ave = MoneyTable.monthAverage(m_sum);
        Calendar c = Calendar.getInstance();
//        https://stackoverflow.com/questions/8940438/number-of-days-in-particular-month-of-particular-year
        YearMonth yearMonthObject = YearMonth.of(c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1);
        int days = yearMonthObject.lengthOfMonth();
//        Log.d("days", String.valueOf(days));
        todayOut.setText(String.format(Locale.US, "%d (%d→%d)",
            t_sum, ave, ave*days));
    }

    /**
     * reload(true);
     */
    public void reload(){reload(true);}

    /**
     * リロード
     * @param resetSpin walletSpinをリセットするか否か。引数無しdefaultはtrue
     */
    public void reload(boolean resetSpin){
        Log.d("reload", "start");
        editMoney.getText().clear();
        editMemo.getText().clear();
        if(resetSpin)spnWallet.setSelection(0);
        if(resetSpin)spnWallet2.setSelection(0);
//        ((LinearLayout) findViewById(R.id.tableWrapper)).removeAllViews();
        String strLimit = PreferenceManager.getDefaultSharedPreferences(this).getString("main_readData_limit", "10");
        readData(Integer.parseInt(strLimit));
        setTodaySum();
        Log.d("reload", "end");
    }

//    functionButtonに入れる機能

    /**
     * functionButton
     * 残額表示ダイアログ
     */
    private void fn_checkBalanceDialog(){
        CharSequence[] walletList = MoneySetting.getList(this, MoneySetting.WALLET);
        final View v = this.getLayoutInflater().inflate(R.layout.dialog_button_close, null);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("残額表示")
                .setSingleChoiceItems(walletList, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        new Thread(()->{
                            String selected = walletList[i].toString();
                            String balanceText = "¥"+MoneyTable.getBalanceOf(MainActivity.this, selected);
                            handler.post(()-> Snackbar.make(v, balanceText, Snackbar.LENGTH_LONG)
                                    .setAction("残高調整", view -> fn_checkBalanceDialog_edit(selected)).show()
                            );
                        }).start();
                    }
                })
                .setView(v).create();
        v.findViewById(R.id.dialog_button).setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    private void fn_checkBalanceDialog_edit(String wallet){
        int balanceNow = MoneyTable.getBalanceOf(this, wallet);
        EditText editText = new EditText(this);
        editText.setHint("実際の残高");
        editText.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(this).setTitle("残高調整: "+wallet)
                .setMessage("Data: ¥"+balanceNow+" ->")
                .setView(editText)
                .setPositiveButton("実行", (dialogInterface, i) -> {
                    int balanceNew = myTool.getNullableInt(editText);
                    int diff = balanceNew - balanceNow;
                    if(diff==0)return;
                        new AsyncInsert(this, this::reload)
                                .execute(new InsertParams(
                                        null, null, null, balanceNew,
                                        wallet, "残高調整", String.format(Locale.US, "%+d", diff)
                                ));
                })
                .setNeutralButton("取消", null)
                .show();
    }

    /**
     * functionButton
     * 削除するidの受付
     */
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

    /**
     * 実際に削除するダイアログ
     * @param id 削除するデータのid
     */
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
                                handler.post(()-> reload());
                            }).start();
                        }
                    }).setNeutralButton("削除しない", null).show();
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }


    }
}