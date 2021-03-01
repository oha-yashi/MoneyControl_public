package com.example.moneycontrol;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceFragmentCompat;


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
            findPreference("delete").setOnPreferenceClickListener((preference)->{
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("建設中").setMessage("少々お待ちください m(_ _)m").show();
                return false;
            });
            findPreference("csvExport").setOnPreferenceClickListener(preference -> {
                // https://developer.android.com/training/sharing/send?hl=ja
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);

                StringBuilder csv = new StringBuilder();
                SQLiteDatabase db = MoneyTableOpenHelper.databaseNullCheck(getActivity(), null);//getActivityでfragmentの所属するActivityが返るので実質this
                Cursor cursor = db.rawQuery("SELECT * FROM "+ MoneyTableOpenHelper.TABLE_NAME, null);
                csv.append(String.join(",", cursor.getColumnNames())).append("\n");
                cursor.moveToFirst();
                int columns = cursor.getColumnCount();
                for(int i=0; i<cursor.getCount(); i++){
                    for(int j=0; j<columns; j++){
                        csv.append(cursor.getString(j));
                        csv.append(",");
                    }
                    csv.deleteCharAt(csv.length()-1);
                    csv.append("\n");
                    cursor.moveToNext();
                }
                cursor.close();

                sendIntent.putExtra(Intent.EXTRA_TEXT, csv.toString());
                sendIntent.setType("text/plain");

                Intent shareIntent = Intent.createChooser(sendIntent, null);
                startActivity(shareIntent);

                return false;
            });
        }
    }
}