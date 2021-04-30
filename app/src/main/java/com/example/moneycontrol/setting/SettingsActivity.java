package com.example.moneycontrol.setting;

import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Adapter;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.moneycontrol.myTool;
import com.example.moneycontrol.sqliteopenhelper.AsyncInsert;
import com.example.moneycontrol.sqliteopenhelper.InsertParams;
import com.example.moneycontrol.sqliteopenhelper.MoneySetting;
import com.example.moneycontrol.sqliteopenhelper.MoneyTable;
import com.example.moneycontrol.R;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;


public class SettingsActivity extends AppCompatActivity {
    private static Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
//        ActionBar actionBar = getSupportActionBar();
//        if (actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true); //アクションバーに戻る矢印を出す
        handler = new Handler(Looper.getMainLooper());
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            findPreference("show_all").setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getContext(), setting_showall.class));
                return false;
            });

            EditTextPreference editReadDataLimit = findPreference("main_readData_limit");
            if (editReadDataLimit != null) {
                editReadDataLimit.setOnBindEditTextListener(
                        new EditTextPreference.OnBindEditTextListener() {
                            @Override
                            public void onBindEditText(@NonNull EditText editText) {
                                editText.setInputType(InputType.TYPE_CLASS_NUMBER);
                            }
                        });
            }

            /*
                データベース操作
             */

            findPreference("delete").setOnPreferenceClickListener((preference) -> {
                String tableName = MoneyTable.getTodayTableName();
                new AlertDialog.Builder(requireContext()).setTitle("テーブル"+tableName+"全削除").setMessage("取り消しできません 消去しますか？")
                        .setPositiveButton("削除", (dialogInterface, i) -> {
                            deleteTable(requireContext());
                        })
                        .setNegativeButton("削除しません", null)
                        .show();
                return false;
            });

            findPreference("export").setOnPreferenceClickListener(preference -> {
                Spinner s = new Spinner(requireContext());
                s.setAdapter(new ArrayAdapter<>(
                        requireContext(),
                        R.layout.support_simple_spinner_dropdown_item,
                        MoneyTable.getMoneyTableNames(requireContext())
                ));
                new AlertDialog.Builder(requireContext()).setTitle("テーブル選択").setView(s)
                        .setPositiveButton("選択",(dialogInterface, i) -> {
                            String[] list = new String[]{"csv", "markdown"};
                            final int[] selected = {-1};
                            new AlertDialog.Builder(requireContext()).setTitle("エクスポート")
                                    .setSingleChoiceItems( //setMessageは共存できない
                                            list, selected[0], new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialogInterface, int i) {
                                                    selected[0] = i;
                                                }
                                            })
                                    .setPositiveButton("決定", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            textExport(list[selected[0]], ((String) s.getSelectedItem()));
                                        }
                                    }).show();
                        })
                        .show();
                return false;
            });

            findPreference("csvImport").setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), readCSV.class));
//                deleteTable(requireContext()); //readCSVの中でやる
                return false;
            });

            findPreference("get_balance").setOnPreferenceClickListener(preference -> get_balance());

            /*
                選択項目編集
             */

            Preference.OnPreferenceClickListener editMoneySetting = new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    int itemNum;
                    for(itemNum = MoneySetting.INCOME; itemNum<=MoneySetting.OUTGO; itemNum++)if(preference.getKey().equals(MoneySetting.TABLE_NAME[itemNum]))break;
                    myTool.MyLog.d(String.valueOf(itemNum));
                    editMoneySetting_Dialog(itemNum);
                    return false;
                }
            };

            for(int i=MoneySetting.INCOME; i<=MoneySetting.WALLET; i++)findPreference(MoneySetting.TABLE_NAME[i]).setOnPreferenceClickListener(editMoneySetting);

            /*
                その他
             */

            new Thread(()->{
                String getTodayTableName = MoneyTable.getTodayTableName();
                handler.post(()-> findPreference("database_table").setSummary(getTodayTableName));
            }).start();

//            テストスペース
            Preference p = findPreference("prefTest");
            Preference.OnPreferenceClickListener pc = preference -> {
                String strTime = myTool.toTimestamp(Calendar.getInstance());
                preference.setSummary(strTime);
                return false;
            };
            if (true) { // TODO: テストしないときここfalse
                if (p != null) {
                    p.setSummary(MoneyTable.getExistTableNames(getActivity()));
                }
            } else {
                p.setVisible(false);
            }

            findPreference("openSetting").setOnPreferenceClickListener(preference -> {
                startActivity(new Intent().setAction(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
                return false;
            });
        }

        //        テストスペースに、joinedColumnをコピペできるダイアログを出す
        private void prefTest(Preference p) {
            EditText e = new EditText(getActivity());
            e.setText(MoneyTable.getColumnsJoined());
            p.setOnPreferenceClickListener(preference -> {
                Log.d("p#onClick", "run");
                new AlertDialog.Builder(requireActivity()).setTitle("get columns")
                        .setView(e).show();
                return false;
            });
        }

        /**
         * 現在のテーブルの書き出し
         * @param type "csv", "markdown"
         */
        private void textExport(String type, String table_name) {
            // https://developer.android.com/training/sharing/send?hl=ja
            Intent sendIntent = new Intent();
            StringBuilder exportText = new StringBuilder();
            boolean isCSV = type.equals("csv");
            boolean isMarkdown = type.equals("markdown");
            myTool.MyLog.d(type);
            new Thread(() -> {
                sendIntent.setAction(Intent.ACTION_SEND);

                SQLiteDatabase db = MoneyTable.newDatabase(getActivity());
                //                getActivityでfragmentの所属するActivityが返るので実質this
                Cursor cursor = db.rawQuery(MoneyTable.QUERY_SELECT_ALL(table_name), null);

                if (isCSV) {
                    exportText.append("#").append(table_name).append("\n");
                    exportText.append(String.join(",", cursor.getColumnNames())).append("\n");
                }
                if (isMarkdown) {
                    exportText.append("# "+table_name+"\n");
                    exportText.append("|").append(String.join("|", cursor.getColumnNames())).append("|\n")
                            .append("|--:|---|--:|--:|--:|:-:|:-:|:--|\n");
                }
                cursor.moveToFirst();
                int columns = cursor.getColumnCount();
                for (int i = 0; i < cursor.getCount(); i++) {
                    if (isMarkdown) exportText.append("|");
                    for (int j = 0; j < columns; j++) {
                        String getS = cursor.getString(j);
                        exportText.append(myTool.nullToSpace(getS));
                        if (isCSV) exportText.append(",");
                        if (isMarkdown) exportText.append("|");
                    }
                    if (isCSV) exportText.deleteCharAt(exportText.length() - 1);
                    exportText.append("\n");
                    cursor.moveToNext();
                }
                cursor.close();
                db.close();

                sendIntent.putExtra(Intent.EXTRA_TEXT, exportText.toString());
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);
            }).start();
        }


        private void deleteTable(Context context){
            String tableName = MoneyTable.getTodayTableName();
            try(SQLiteDatabase sqLiteDatabase = MoneyTable.newDatabase(context)) {
                sqLiteDatabase.execSQL(MoneyTable.QUERY_DROP(tableName));
                sqLiteDatabase.execSQL(MoneyTable.QUERY_CREATE(tableName));
            }
        }

        private void editMoneySetting_Dialog(int item){
            Pair<List<Integer>,List<String>> lists = MoneySetting.getList(requireContext(), item);
            List<String> margeList = new ArrayList<>();
            for(int i=0; i<lists.first.size(); i++)margeList.add(lists.first.get(i) + ":" + lists.second.get(i));
            new AlertDialog.Builder(requireContext())
                    .setItems(margeList.toArray(new String[0]), (dialogInterface, i) -> {
                        editMoneySetting_editList(item, Pair.create(lists.first.get(i),lists.second.get(i)), lists.first.size());
                    })
                    .setPositiveButton("追加", (dialogInterface, i) -> {
                        editMoneySetting_addList(item, lists.first.size());
                    })
                    .setNeutralButton("閉じる", null).show();
        }

        private void editMoneySetting_editList(int item, Pair<Integer,String> pn, int maxPriority){
            final View v = this.getLayoutInflater().inflate(R.layout.editmoneysetting_editlist,null);
            EditText p = v.findViewById(R.id.MS_priority);
            EditText n = v.findViewById(R.id.MS_name);
            String name = pn.second;
            Integer pri = pn.first;
            if(pri==null)pri=maxPriority;
            p.setText(String.valueOf(pri));
            n.setText(name);

            int finalPri = pri;
            new AlertDialog.Builder(requireContext()).setTitle("項目編集")
                    .setView(v)
                    .setPositiveButton("変更", (dialogInterface, i) -> {
                        if(TextUtils.isEmpty(n.getText()) || TextUtils.isEmpty(p.getText()))return;
                        int p_to = Integer.parseInt(p.getText().toString());
                        try(SQLiteDatabase db = new MoneySetting(requireContext()).getWritableDatabase()){
                            if(finalPri !=p_to) for(String q:MoneySetting.QUERY_MOVE_PRIORITY(item,name, finalPri,p_to)) {
                                Log.d("SA.editMoneySetting_editList",q);
                                db.execSQL(q);
                            }
                            db.execSQL(MoneySetting.QUERY_UPDATE(item, name, n.getText().toString()));
                        }
                    })
                    .setNegativeButton("削除",(dialogInterface, i) -> {
                        if(TextUtils.isEmpty(n.getText()))return;
                        try(SQLiteDatabase db = new MoneySetting(requireContext()).getWritableDatabase()){
                            String q = String.format(Locale.US,"UPDATE %s SET priority=(priority-1) WHERE priority>%d",MoneySetting.TABLE_NAME[item],finalPri);
                            db.execSQL(MoneySetting.QUERY_DELETE(item, n.getText().toString()));
                            db.execSQL(q);
                        }
                    })
                    .setNeutralButton("閉じる",null).show();
        }

        private void editMoneySetting_addList(int item, int maxPriority){
            EditText editText = new EditText(requireContext());
            new AlertDialog.Builder(requireContext()).setTitle("項目追加")
                    .setView(editText)
                    .setPositiveButton("追加", (dialogInterface, i) -> {
                        if(TextUtils.isEmpty(editText.getText()))return;
                        try(SQLiteDatabase db = new MoneySetting(requireContext()).getWritableDatabase()){
                            ContentValues cv = new ContentValues();
                            cv.put("name", editText.getText().toString());
                            cv.put("priority",maxPriority);
                            db.insert(MoneySetting.TABLE_NAME[item],null,cv);
                        }
                    })
                    .setNeutralButton("閉じる",null).show();
        }

        private boolean get_balance(){
            Context context = requireContext();
            Calendar c = Calendar.getInstance();
            String tableNow = MoneyTable.getCalendarTableName(c);
            c.add(Calendar.MONTH,-1);
            String tablePre = MoneyTable.getCalendarTableName(c);
            List<String> wallets = MoneySetting.getList(context, 2).second;
            List<Integer> list = new ArrayList<>();
            new AlertDialog.Builder(context).setTitle(tablePre+" -> "+tableNow)
                .setMultiChoiceItems(wallets.toArray(new CharSequence[0]), null, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i, boolean b) {
                        if(b)list.add(i);
                        if(!b)list.removeIf(Predicate.isEqual(i));
                    }
                })
                .setPositiveButton("実行",(dialogInterface, i) -> {
                    for(int ii:list){
                        String selected_wallet = wallets.get(ii);
//                                Log.d("get_balance",selected_wallet);
                        int nowHaveData = MoneyTable.countData(context,tableNow,"wallet",selected_wallet);
                        int preHaveData = MoneyTable.countData(context,tablePre,"wallet",selected_wallet);
//                                Log.d("get_balance",String.format(Locale.US,"now=%d,pre=%d",nowHaveData,preHaveData));
                        if(nowHaveData==0 && preHaveData>0){
                            int pre_balance = MoneyTable.getBalanceOf(context,tablePre,selected_wallet);
                            new AsyncInsert(context, null).execute(new InsertParams(
                                    null,null,null,pre_balance,
                                    selected_wallet,"残高調整", "from " + tablePre
                            ));
                        }
                    }
                }).show();
            return false;
        }
    }
}