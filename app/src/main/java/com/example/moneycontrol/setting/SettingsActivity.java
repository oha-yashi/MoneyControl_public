package com.example.moneycontrol.setting;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.moneycontrol.sqliteopenhelper.MoneySetting;
import com.example.moneycontrol.sqliteopenhelper.MoneyTable;
import com.example.moneycontrol.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class SettingsActivity extends AppCompatActivity {
    private Handler handler;

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
            findPreference("show_balance").setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                CharSequence[] walletList = MoneySetting.getList(requireContext(), MoneySetting.WALLET);
                builder.setTitle("残額表示")
                        .setItems(walletList, (dialogInterface, i) -> {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(requireContext());
                            String getWallet = (String) walletList[i];
                            int getBalance = MoneyTable.getBalanceOf(requireContext(), getWallet);
                            builder1.setTitle("残額 of " + getWallet)
                                    .setMessage(getBalance + "円").show();
                        }).show();
                return false;
            });
            findPreference("show_balance").setSummary("各walletの残高を表示します");
            findPreference("delete").setOnPreferenceClickListener((preference) -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("テーブル全削除").setMessage("取り消しできません 消去しますか？")
                        .setPositiveButton("削除", (dialogInterface, i) -> {
                            SQLiteDatabase sqLiteDatabase = MoneyTable.newDatabase(getContext());
                            sqLiteDatabase.execSQL(MoneyTable.SQL_DELETE_QUERY);
                            sqLiteDatabase.execSQL(MoneyTable.SQL_CREATE_QUERY);
                            new AlertDialog.Builder(requireContext()).setMessage("再起動してください").show();
                        })
                        .setNegativeButton("削除しません", null)
                        .show();
                return false;
            });


            findPreference("export").setOnPreferenceClickListener(preference -> {
                String[] list = new String[]{"csv", "markdown"};
                final int[] selected = {-1};
                new AlertDialog.Builder(getActivity()).setTitle("エクスポート")
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
                return false;
            });

            findPreference("database_table").setSummary(MoneyTable.getTodayTableName());

            findPreference("prefTest").setSummary(MoneyTable.getColumnsJoined());

//            テストスペース
            Preference p = findPreference("prefTest");
            Preference.OnPreferenceClickListener pc = preference -> {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
//                SimpleDateFormat sdf = new SimpleDateFormat("'Y'yyyy'M'MM", Locale.US);
                String strTime = sdf.format(Calendar.getInstance().getTime());
                preference.setSummary(strTime);
                return false;
            };
            if (!true) { // TODO: テストしないときここfalse
//                prefTest(p);
/*                SQLiteDatabase sqLiteDatabase = MoneyTable.newDatabase(getActivity());
                String[] AS = {"IncomeGenre", "OutgoGenre", "Wallet"};
                for(String s: AS) {
                    Log.d("testDROP", s);
                    sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+s);
                }*/
                p.setOnPreferenceClickListener(pc);
            } else {
                p.setVisible(false);
            }
        }

        //        テストスペースに、joinedColumnをコピペできるダイアログを出す
        private void prefTest(Preference p) {
            EditText e = new EditText(getActivity());
            e.setText(MoneyTable.getColumnsJoined());
            p.setOnPreferenceClickListener(preference -> {
                Log.d("p#onClick", "run");
                new AlertDialog.Builder(getActivity()).setTitle("get columns")
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
                Cursor cursor = db.rawQuery(MoneyTable.READ_ALL_QUERY, null);
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
    }
}