package com.example.moneycontrol;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

import static android.os.SystemClock.sleep;

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

    private MCOpenHelper helper;
    private SQLiteDatabase db;

    private enum IOM {INCOME, OUTGO, MOVE}

    private boolean isMove; //資金移動かどうか

    public void databaseNullCheck(){
        if(helper==null){
            Log.d("databaseNullCheck", "helper is Null");
            helper = new MCOpenHelper(this);
        }else Log.d("databaseNullCheck", "helper is not Null");
        if(db==null){
            Log.d("databaseNullCheck", "db is Null");
            db = helper.getWritableDatabase();
        }else Log.d("databaseNullCheck", "db is not Null");
    }

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
        databaseNullCheck();

        {
            //thread test
            Log.d("thread", "before");
            new Thread(() -> {
                Log.d("new thread", "run");
                sleep(1000);
                Log.d("new thread", "end");
            }).start();
            Log.d("thread", "after");
        }


        L_memo = findViewById(R.id.memoLayout);
        L_move = findViewById(R.id.moveLayout);
        L_btn  = findViewById(R.id.ioButtonLayout);
        I_arrow = findViewById(R.id.downArrow);
        //buttonIncome
        Button btn_in = findViewById(R.id.buttonIncome);
        //buttonOutgo
        Button btn_out = findViewById(R.id.buttonOutgo);
        btn_move = findViewById(R.id.moveButton);

        //入力欄からfocusが外れたらキーボードを消す
        editMoney.setOnFocusChangeListener((view, hasFocus) -> {
            if(!hasFocus){
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });
        editMemo.setOnFocusChangeListener((view, hasFocus) -> {
            if(!hasFocus){
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        });

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

        //spinnerにwalletを設定する
        List<String> LS = helper.tableColumnToArray(db, MCOpenHelper.WALLET_TABLE, "name");
        spnWallet.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, LS));
        spnWallet2.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, LS));
        setTodaySum();
        readData();
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
                    List<String> list = helper.tableColumnToArray(
                            db, isIncome ? MCOpenHelper.INCOME_GENRE_TABLE : MCOpenHelper.OUTGO_GENRE_TABLE,
                            "name");
                    String[] item = list.toArray(new String[0]);
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
    private void buttonSizeScale(Button btn, float scale){
        btn.setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimension(R.dimen.buttonTextSize)*scale);
        //getResourcesがpx値を返すので単位変換をする
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

            databaseNullCheck();

            String text_move = getString(R.string.button_move);
            String st_in = getString(R.string.status_income);
            String st_out = getString(R.string.status_outgo);

            String status = iom == IOM.INCOME ? st_in
                    : iom == IOM.OUTGO ? st_out
                    : /*iom==IOM.MOVE*/ "";
            String wallet = (String) spnWallet.getSelectedItem();
            String wallet2 = (String) spnWallet2.getSelectedItem();
            String note = editMemo.getText().toString();

            //収入支出 資金移動from側の書き込み

            ContentValues cv = new ContentValues();
            cv.put("status", status);
            cv.put("money", (iom == IOM.INCOME ? "" : "-") + money);//支出と資金移動のfrom項目ではマイナスにする
            cv.put("wallet", wallet);
            cv.put("genre", iom==IOM.MOVE ? text_move
                    : genre.isEmpty() ? "" : genre);
            cv.put("note", iom==IOM.MOVE ? "to "+wallet2 : note);
            //ジャンルはnullで始めてあとからボタン押下時選択項目出るよう実装
            long id = db.insert(MCOpenHelper.TABLE_NAME, null, cv);
            Log.d("iomButton", cv.toString());

            //資金移動toの書き込み
            if(iom == IOM.MOVE){
                cv.clear();
                cv.put("status", status);
                cv.put("money", money);
                cv.put("wallet", wallet2);
                cv.put("genre", text_move);
                cv.put("note", "from "+wallet);
                db.insert(MCOpenHelper.TABLE_NAME, null, cv);
                Log.d("iomButton", cv.toString());
            }
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
        Log.d("move", "button pressed");
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
        Log.d("moveDo", "button pressed");

        if(!isMove){
            //ボタンが表示されたときはisMove=trueなはずなのでないはず
            Log.d("moveDo", "why isMove=true???");
        }else{
            //正常処理
            Log.d("movedo", "do Move");
            iomButton(IOM.MOVE, null);

            fromMove();
        }
    }

    /**
     * 直近の項目を削除する
     * @param v
     */
    public void undoButton(View v){
        Log.d("undoButton", "clicked");
        databaseNullCheck();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        StringBuilder lastItem = new StringBuilder("|");
        Cursor cursor = db.rawQuery(MCOpenHelper.READ_ALL_QUERY, null);
        cursor.moveToLast();
        for(int i=1; i<=6; i++){
            lastItem.append(cursor.getString(i)).append("|");
        }
        cursor.close();
        builder.setTitle("直近項目削除")
                .setMessage(lastItem.toString());
        builder.show();
    }

    private void readData(){
        databaseNullCheck();

        Cursor cursor = db.rawQuery(MCOpenHelper.READ_ALL_QUERY, null);

        //読み取り

        cursor.moveToLast();

        int readCount = Integer.min(cursor.getCount(), 5);
        TextView tv;

        for(int i=0; i<readCount; i++){
            //sb.append(cursor.getInt(0)); sb.append(" "); //最初は_idなので読まない

            String idDate = "tableDate" + (i+1); //Integer.toStringは不要
            tv = findViewById(getResources().getIdentifier(idDate, "id", getPackageName()));
            tv.setText(cursor.getString(1).substring(5, 16));
            //idDateの文字列でidを検索してfindViewByIdに送る
            //そのidで取得したviewに対応する値を入れる
            tv = findViewById(getResources().getIdentifier("tableStatus" + (i+1), "id", getPackageName()));
            tv.setText(cursor.getString(2));
            tv = findViewById(getResources().getIdentifier("tableMoney"+(i+1), "id", getPackageName()));
            tv.setText(cursor.getString(3));
            tv = findViewById(getResources().getIdentifier("tableWallet"+(i+1), "id", getPackageName()));
            tv.setText(cursor.getString(4));
            tv = findViewById(getResources().getIdentifier("tableMemo"+(i+1), "id", getPackageName()));
            String g = cursor.getString(5);
            String n = cursor.getString(6);
            //genreない時すぐnote ある時genre : note
            if(g.isEmpty() || n.isEmpty()){
                tv.setText(String.format("%s%s", g, n));
            }else{
                tv.setText(String.format("%s : %s", g, n));
            }

            cursor.moveToPrevious();
        }
        cursor.close();
    }

    private void setTodaySum(){todayOut.setText(String.format(Locale.US, "%d", todaySum()));}

    /**
     * 今日の支出
     * @return int 支出
     */
    private int todaySum(){
        int sum = 0;
        databaseNullCheck();

        String SEARCH_TODAYSUM_QUERY = "select total(-money) from " + MCOpenHelper.TABLE_NAME
                + " where strftime('%m%d', timestamp) = strftime('%m%d', 'now', 'localtime') and status = '"
                + getString(R.string.status_outgo) + "'";
        Cursor c = db.rawQuery(SEARCH_TODAYSUM_QUERY, null);
        c.moveToFirst();
        for(int i=0; i<c.getCount(); i++)sum += c.getInt(0);
        c.close();

        return sum;
    }

    public void settingButton(View v){
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
}

//hoge