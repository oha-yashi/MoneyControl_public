package com.example.moneycontrol;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Objects;

public class SettingsActivity extends AppCompatActivity {

    private MCOpenHelper helper;
    private SQLiteDatabase db;

    FloatingActionButton fab;

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
                Intent intent = new Intent(getContext(), setting_showall.class);
                startActivity(intent);
                return false;
            });
            findPreference("delete").setOnPreferenceClickListener((preference)->{
                AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                builder.setTitle("建設中").setMessage("少々お待ちください m(_ _)m").show();
                return false;
            });
            //ここでpreferenceのsetOnClickする
        }
    }
}