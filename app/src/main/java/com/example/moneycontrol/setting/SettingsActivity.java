package com.example.moneycontrol.setting;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.moneycontrol.MoneySettingOpenHelper;
import com.example.moneycontrol.MoneyTableOpenHelper;
import com.example.moneycontrol.R;
import com.example.moneycontrol.setting.setting_showall;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;


public class SettingsActivity extends AppCompatActivity {
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
    }

    public static class SettingsFragment extends PreferenceFragmentCompat{
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            findPreference("show_all_button").setOnPreferenceClickListener(preference -> {
                startActivity(new Intent(getContext(), setting_showall.class));
                return false;
            });
            findPreference("show_balance").setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                CharSequence[] walletList = MoneySettingOpenHelper.getList(requireContext(), MoneySettingOpenHelper.WALLET);
                builder.setTitle("残額表示")
                        .setItems(walletList, (dialogInterface, i) -> {
                            AlertDialog.Builder builder1 = new AlertDialog.Builder(requireContext());
                            String getWallet = (String) walletList[i];
                            int getBalance = MoneyTableOpenHelper.getBalanceOf(requireContext(), getWallet);
                            builder1.setTitle("残額 of "+getWallet)
                                    .setMessage(getBalance+"円").show();
                        }).show();
                return false;
            });
            findPreference("show_balance").setSummary("各walletの残高を表示します");
            findPreference("delete").setOnPreferenceClickListener((preference)->{
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("テーブル全削除").setMessage("取り消しできません 消去しますか？")
                        .setPositiveButton("削除", (dialogInterface, i) -> {
                            SQLiteDatabase sqLiteDatabase = MoneyTableOpenHelper.newDatabase(getContext());
                            sqLiteDatabase.execSQL(MoneyTableOpenHelper.SQL_DELETE_QUERY);
                            sqLiteDatabase.execSQL(MoneyTableOpenHelper.SQL_CREATE_QUERY);
                            new AlertDialog.Builder(requireContext()).setMessage("再起動してください").show();
                        })
                        .setNegativeButton("削除しません", null)
                        .show();
                return false;
            });
            findPreference("csvExport").setOnPreferenceClickListener(preference -> {
                // https://developer.android.com/training/sharing/send?hl=ja
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);

                StringBuilder csv = new StringBuilder();
                SQLiteDatabase db = MoneyTableOpenHelper.newDatabase(getActivity());
//                getActivityでfragmentの所属するActivityが返るので実質this
                Cursor cursor = db.rawQuery(MoneyTableOpenHelper.READ_ALL_QUERY, null);
                csv.append(String.join(",", cursor.getColumnNames())).append("\n");
                cursor.moveToFirst();
                int columns = cursor.getColumnCount();
                for(int i=0; i<cursor.getCount(); i++){
                    for(int j=0; j<columns; j++){
                        String getS = cursor.getString(j);
                        csv.append(getS
                                ==null?"":getS
                        );
                        csv.append(",");
                    }
                    csv.deleteCharAt(csv.length()-1);
                    csv.append("\n");
                    cursor.moveToNext();
                }
                cursor.close();
                db.close();

                sendIntent.putExtra(Intent.EXTRA_TEXT, csv.toString());
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);

                return false;
            });

            findPreference("csvImport").setOnPreferenceClickListener(preference -> {
//                new AlertDialog.Builder(getActivity()).setTitle("建設中")
//                        .setMessage("csv読み込みテストしてから実装").show();
//                openFile(); // 呼び出したいけどここがstaticなのでnonstaticなのは無理
                startActivity(new Intent(getActivity(), readCSV.class));
                return false;
            });

            findPreference("database_table").setSummary(MoneyTableOpenHelper.getTableName());

            findPreference("prefTest").setSummary(MoneyTableOpenHelper.getColumnsJoined());

//            テストスペース
            Preference p = findPreference("prefTest");
            if (false) { // TODO: テストしないときここfalse
                prefTest(p);
            } else {
                p.setVisible(false);
            }
        }
//        テストスペースに、joinedColumnをコピペできるダイアログを出す
        private void prefTest(Preference p){
            EditText e = new EditText(getActivity());
            e.setText(MoneyTableOpenHelper.getColumnsJoined());
            p.setOnPreferenceClickListener(preference -> {
                Log.d("p#onClick", "run");
                new AlertDialog.Builder(getActivity()).setTitle("get columns")
                        .setView(e).show();
                return false;
            });
        }
    }
}