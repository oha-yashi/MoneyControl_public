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
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.moneycontrol.myTool;
import com.example.moneycontrol.sqliteopenhelper.MoneySetting;
import com.example.moneycontrol.sqliteopenhelper.MoneyTable;
import com.example.moneycontrol.R;

import org.w3c.dom.Text;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;


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
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        handler = new Handler(Looper.getMainLooper());
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            findPreference("show_all_button").setOnPreferenceClickListener(preference -> {
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
                                textExport(list[selected[0]]);
                            }
                        }).show();
                return false;
            });

            findPreference("csvImport").setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getActivity(), readCSV.class));
//                deleteTable(requireContext()); //readCSVの中でやる
                return false;
            });

            /*
                選択項目編集
             */

            Preference.OnPreferenceClickListener editMoneySetting = new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    int itemNum;
                    for(itemNum = MoneySetting.INCOME; itemNum<=MoneySetting.OUTGO; itemNum++)if(preference.getKey().equals(MoneySetting.TABLE_NAME[itemNum]))break;
                    Log.d("itemNum", String.valueOf(itemNum));
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
                handler.post(()->{
                    findPreference("database_table").setSummary(getTodayTableName);
                });
            }).start();

//            テストスペース
            Preference p = findPreference("prefTest");
            Preference.OnPreferenceClickListener pc = preference -> {
                String strTime = myTool.toTimestamp(Calendar.getInstance());
                preference.setSummary(strTime);
                return false;
            };
            if (true) { // TODO: テストしないときここfalse
//                prefTest(p);
/*                SQLiteDatabase sqLiteDatabase = MoneyTable.newDatabase(getActivity());
                String[] AS = {"IncomeGenre", "OutgoGenre", "Wallet"};
                for(String s: AS) {
                    Log.d("testDROP", s);
                    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+s);
                }*/
//                p.setOnPreferenceClickListener(pc);
            /*    p.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        new AlertDialog.Builder(getActivity()).setTitle("test")
                                .setMessage("テストinsert")
                                .setPositiveButton("Do", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialogInterface, int i) {
                                        MoneyTable.insert(getActivity(),null,
                                                123,null,999,
                                                "テストwallet","テストgenre", "テストnote");
                                    }
                                }).show();
                        return false;
                    }
                });*/
                p.setSummary(MoneyTable.getExistTableNames(getActivity()));
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
        private void textExport(String type) {
            // https://developer.android.com/training/sharing/send?hl=ja
            Intent sendIntent = new Intent();
            StringBuilder exportText = new StringBuilder();
            boolean isCSV = type.equals("csv");
            boolean isMarkdown = type.equals("markdown");
            Log.d("textExport", type);
            new Thread(() -> {
                sendIntent.setAction(Intent.ACTION_SEND);

                SQLiteDatabase db = MoneyTable.newDatabase(getActivity());
                //                getActivityでfragmentの所属するActivityが返るので実質this
                Cursor cursor = db.rawQuery(MoneyTable.QUERY_SELECT_ALL(MoneyTable.getTodayTableName()), null);
                if (isCSV)
                    exportText.append(String.join(",", cursor.getColumnNames())).append("\n");
                if (isMarkdown) {
                    exportText.append("|").append(String.join("|", cursor.getColumnNames())).append("|\n")
                            .append("|--:|---|--:|--:|--:|:-:|:-:|:--|\n");
                }
                cursor.moveToFirst();
                int columns = cursor.getColumnCount();
                for (int i = 0; i < cursor.getCount(); i++) {
                    if (isMarkdown) exportText.append("|");
                    for (int j = 0; j < columns; j++) {
                        String getS = cursor.getString(j);
                        exportText.append(TextUtils.isEmpty(getS) ? " " : getS);
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
            List<String> list = MoneySetting.getList(requireContext(), item).second;
            new AlertDialog.Builder(requireContext())
                    .setItems(list.toArray(new String[0]), (dialogInterface, i) -> {
                        editMoneySetting_editList(item, list.get(i));
                    })
                    .setPositiveButton("追加", (dialogInterface, i) -> {
                        editMoneySetting_addList(item);
                    })
                    .setNeutralButton("閉じる", null).show();
        }

        private void editMoneySetting_editList(int item, String name_priority){
            final View v = this.getLayoutInflater().inflate(R.layout.editmoneysetting_editlist,null);
            EditText p = v.findViewById(R.id.MS_priority);
            EditText n = v.findViewById(R.id.MS_name);
            String name = name_priority.split(":")[1];
            String pri = name_priority.split(":")[0];
            if(Objects.equals(pri, "null"))pri="-1";
            p.setText(pri);
            n.setText(name);
            int p_from = Integer.parseInt(pri);

            new AlertDialog.Builder(requireContext()).setTitle("項目編集")
                    .setView(v)
                    .setPositiveButton("変更", (dialogInterface, i) -> {
                        if(TextUtils.isEmpty(n.getText()) || TextUtils.isEmpty(p.getText()))return;
                        int p_to = Integer.parseInt(p.getText().toString());
                        try(SQLiteDatabase db = new MoneySetting(requireContext()).getWritableDatabase()){
                            if(p_from!=p_to) for(String q:MoneySetting.QUERY_MOVE_PRIORITY(item,name,p_from,p_to)) {
                                Log.d("SA.editMoneySetting_editList",q);
                                db.execSQL(q);
                            }
                            db.execSQL(MoneySetting.QUERY_UPDATE(item, name, n.getText().toString()));
                        }
                    })
                    .setNegativeButton("削除",(dialogInterface, i) -> {
                        if(TextUtils.isEmpty(n.getText()))return;
                        try(SQLiteDatabase db = new MoneySetting(requireContext()).getWritableDatabase()){
                            String q = String.format(Locale.US,"UPDATE %s ",MoneySetting.TABLE_NAME[item]);
                            db.execSQL(MoneySetting.QUERY_DELETE(item, n.getText().toString()));
                        }
                    })
                    .setNeutralButton("閉じる",null).show();
        }

        private void editMoneySetting_addList(int item){
            EditText editText = new EditText(requireContext());
            new AlertDialog.Builder(requireContext()).setTitle("項目追加")
                    .setView(editText)
                    .setPositiveButton("追加", (dialogInterface, i) -> {
                        if(TextUtils.isEmpty(editText.getText()))return;
                        try(SQLiteDatabase db = new MoneySetting(requireContext()).getWritableDatabase()){
                            ContentValues cv = new ContentValues();
                            cv.put("name", editText.getText().toString());
                            cv.put("priority",99);
                            db.insert(MoneySetting.TABLE_NAME[item],null,cv);
                        }
                    })
                    .setNeutralButton("閉じる",null).show();
        }
    }
}