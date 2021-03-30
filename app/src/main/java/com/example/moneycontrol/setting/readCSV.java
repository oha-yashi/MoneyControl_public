package com.example.moneycontrol.setting;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.moneycontrol.MoneyTableOpenHelper;
import com.example.moneycontrol.R;
import com.google.android.material.snackbar.Snackbar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class readCSV extends AppCompatActivity {
//
//    read csv file
//

    protected void onCreate(Bundle bundle){
        super.onCreate(bundle);
//
//        レイアウトを持たないアクティビティ
//
        openFile();
    }

    private static final int READ_MONEY_CSV = 4;
    public void openFile(){
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("text/csv"); //csv読み込みのつもりがうまく動かない
        intent.setType("text/*"); //テキスト読み込み
//        intent.setType("*/*"); //なんでも読み込み

        startActivityForResult(intent, READ_MONEY_CSV);
    }

    //    他のActivityから帰ってきたときに呼び出される
    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == READ_MONEY_CSV
                && resultCode == Activity.RESULT_OK) {
            // The result data contains a URI for the document or directory that
            // the user selected.
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                try {
                    getCsvFromUri(uri);
                } catch (IOException e) {
                    e.printStackTrace();
                    Snackbar.make(findViewById(R.id.settings), "IOExceptionError", Snackbar.LENGTH_LONG);
                }
            }
        }
    }

    /**
     * uriで取得したファイルからデータベースに書き込む
     * @param uri
     * @return boolean 書き込みの成否
     * @throws IOException
     */
    private boolean getCsvFromUri(Uri uri) throws IOException {
        SQLiteDatabase sqLiteDatabase = MoneyTableOpenHelper.newDatabase(this);
        try (InputStream inputStream =
                     getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            boolean isFirstLine = true; //これが立っている時はカラム名行の読み込み
            String[] columns = MoneyTableOpenHelper.getColumnsArray();
            while ((line = reader.readLine()) != null) {
//                stringBuilder.append(line).append("\n");
                if(isFirstLine){
                    // 1行目の読み込み
                    if(line.equals(MoneyTableOpenHelper.getColumnsJoined())){
                        // 対応したファイル
                        Toast.makeText(this, "読み込みます", Toast.LENGTH_SHORT);
                        isFirstLine = false;
                    }else{
                        // 不正なファイル
                        Toast.makeText(this, "不正なファイルです", Toast.LENGTH_SHORT);
                        sqLiteDatabase.close();
                        return false;
                    }
                }else{
                    // 2行目以降
                    ContentValues contentValues = new ContentValues();
                    String[] values = line.split(",");
                    for(int i=0; i<values.length; i++){
                        contentValues.put(columns[i], values[i]);
                    }
                    sqLiteDatabase.insert(MoneyTableOpenHelper.getTableName(),null, contentValues);
                }
                //end while
            }
        }
        new AlertDialog.Builder(this).setTitle("End readCSV")
                .setMessage("再起動してください").show();
        sqLiteDatabase.close();
        return true;
    }
}
