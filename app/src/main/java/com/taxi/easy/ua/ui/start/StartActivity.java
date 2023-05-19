package com.taxi.easy.ua.ui.start;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

public class StartActivity extends Activity {
    private static final String DB_NAME = "DbTaxi" ;
    public static final String TABLE_USER_INFO = "userInfo" ;
    public static final String TABLE_SETTINGS_INFO = "settingsInfo" ;
    public static final String TABLE_COMBO_INFO = "comboInfo" ;
    public static SQLiteDatabase database;
    private Cursor cursorDb;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);
        Intent intent =  new Intent(this, MainActivity.class);

        try {
            initDB();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        FloatingActionButton fab = findViewById(R.id.fab);

         fab.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 startActivity(intent);
             }
         });

    }

    private void initDB() throws MalformedURLException, JSONException, InterruptedException {
        database = this.openOrCreateDatabase( DB_NAME , MODE_PRIVATE , null );
        Log.d("TAG", "initDB: " + database);

        database.execSQL( "CREATE TABLE IF NOT EXISTS " + TABLE_USER_INFO + "(id integer primary key autoincrement," +
                " name text," +
                " phone_number text);" );
        database.execSQL( "CREATE TABLE IF NOT EXISTS " + TABLE_COMBO_INFO + "(id integer primary key autoincrement," +
                " name text," +
                " street integer);" );
        database.execSQL( "CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS_INFO + "(id integer primary key autoincrement," +
                " type_auto text," +
                " tarif text);" );

        cursorDb = database.query(TABLE_COMBO_INFO, null, null, null, null, null, null);

        database.delete( TABLE_COMBO_INFO, null , null );
//        database.delete( TABLE_AUTO_INFO, null , null );
    }

    public static void insertRecordsUser(List<String> values){
        String sql = "INSERT INTO " + TABLE_USER_INFO + " VALUES(?,?,?);";
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, values.get(0));
            statement.bindString(3, values.get(1));

            statement.execute();
            database.setTransactionSuccessful();
//            logCursor(TABLE_USER_INFO);
        } finally {
            database.endTransaction();
        }
    }
     public static void insertRecordsCombo(List<String> values){
        String sql = "INSERT INTO " + TABLE_COMBO_INFO + " VALUES(?,?,?);";
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, values.get(0));
            statement.bindString(3, values.get(1));

            statement.execute();
            database.setTransactionSuccessful();
//            logCursor(TABLE_COMBO_INFO);
        } finally {
            database.endTransaction();
        }
    }
    @SuppressLint("Range")
    public static List<String> logCursor(String table) {
        List<String> list = new ArrayList<>();
        Cursor c = database.query(table, null, null, null, null, null, null);
        if (c != null) {
            if (c.moveToFirst()) {
                String str;
                do {
                    str = "";
                    for (String cn : c.getColumnNames()) {
                        str = str.concat(cn + " = " + c.getString(c.getColumnIndex(cn)) + "; ");
                        list.add(c.getString(c.getColumnIndex(cn)));

                    }

                } while (c.moveToNext());
            }
        }
        return list;
    }
}
