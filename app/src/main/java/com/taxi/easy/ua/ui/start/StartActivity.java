package com.taxi.easy.ua.ui.start;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.maps.CostJSONParser;
import com.taxi.easy.ua.ui.maps.Odessa;
import com.taxi.easy.ua.ui.maps.OrderJSONParser;

import org.json.JSONException;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StartActivity extends Activity {
    private static final String DB_NAME = "DataBaseTaxi";
    public static final String TABLE_USER_INFO = "userInfo";
    public static final String TABLE_SETTINGS_INFO = "settingsInfo";

    public static SQLiteDatabase database;
    public static Cursor cursorDb;
    FloatingActionButton fab;
    private String from, to;
    public String region =  "Одеса";
    EditText from_number, to_number;
    String messageResult;


    private static final int READ_PHONE_NUMBERS_CODE = 0;
    private static final int READ_PHONE_STATE_CODE = 0;
    public static final int READ_CALL_PHONE = 0;

    Intent intent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.start_layout);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermission(Manifest.permission.CALL_PHONE,READ_CALL_PHONE);
        checkPermission(Manifest.permission.READ_PHONE_NUMBERS, READ_PHONE_NUMBERS_CODE);
        checkPermission(Manifest.permission.READ_PHONE_STATE, READ_PHONE_STATE_CODE);

            fab = findViewById(R.id.fab);

            intent = new Intent(this, MainActivity.class);
            fab.setVisibility(View.VISIBLE);
            try {
                initDB();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    dialogFromTo();;
                    startActivity(intent);
                }
            });


    }

    private void initDB() throws MalformedURLException, JSONException, InterruptedException {
//        this.deleteDatabase(DB_NAME);
        database = this.openOrCreateDatabase(DB_NAME, MODE_PRIVATE, null);
        Log.d("TAG", "initDB: " + database);

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_USER_INFO + "(id integer primary key autoincrement," +
                " phone_number text);");

        database.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_SETTINGS_INFO + "(id integer primary key autoincrement," +
                " type_auto text," +
                " tarif text);");


        cursorDb = database.query(TABLE_USER_INFO, null, null, null, null, null, null);
        Log.d("TAG", "initDB:" + logCursor(TABLE_USER_INFO));
        phoneNumber(cursorDb.getCount());


        cursorDb = database.query(TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        if (cursorDb.getCount() == 0) {
            List<String> settings = new ArrayList<>();
            settings.add("usually");
            settings.add("Базовый");
            insertFirstSettings(settings);
        } else {
            Log.d("TAG", "initDB:" + logCursor(TABLE_SETTINGS_INFO));
        }

    }

    private void insertFirstSettings(List<String> settings) {
        String sql = "INSERT INTO " + TABLE_SETTINGS_INFO + " VALUES(?,?,?);";
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, settings.get(0));
            statement.bindString(3, settings.get(1));

            statement.execute();
            database.setTransactionSuccessful();

        } finally {
            database.endTransaction();
        }
    }

    public static void insertRecordsUser(String phoneNumber) {
        String sql = "INSERT INTO " + TABLE_USER_INFO + " VALUES(?,?);";
        SQLiteStatement statement = database.compileStatement(sql);
        database.beginTransaction();
        try {
            statement.clearBindings();
            statement.bindString(2, phoneNumber);

            statement.execute();
            database.setTransactionSuccessful();

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


    // Function to check and request permission
    public void checkPermission(String permission, int requestCode) {
        // Checking if permission is not granted
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }
    // This function is called when user accept or decline the permission.
// Request Code is used to check which permission called this function.
// This request code is provided when user is prompt for permission.
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    }

    private void phoneNumber(int curs ) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.phone_verify_layout, null);
        builder.setView(view);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText phoneNumber = view.findViewById(R.id.phoneNumber);
        TelephonyManager tMgr = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    Log.d("TAG", "Manifest.permission.READ_PHONE_NUMBERS: " + ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS));
                    Log.d("TAG", "Manifest.permission.READ_PHONE_STATE: " + ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE));
                return;
        } else {
            @SuppressLint("HardwareIds") String mPhoneNumber = tMgr.getLine1Number();
            Log.d("TAG", "phoneNumber: " + mPhoneNumber);
            phoneNumber.setText(mPhoneNumber);

            if ( curs == 0) {
                builder.setTitle("Перевірка телефону")
                        .setPositiveButton("Відправити", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String urlCost = "https://m.easy-order-taxi.site/api/android/sendCode/" + phoneNumber.getText();
                                Log.d("TAG", "onClick urlCost: " + urlCost);
                                try {
                                    Map sendUrlResult = ResultSONParser.sendURL(urlCost);
                                    Log.d("TAG", "onClick sendUrlMapCost: " + sendUrlResult);
                                    if (sendUrlResult.get("resp_result").equals("200")) {
                                        codeVerify(String.valueOf(phoneNumber.getText()));
                                    } else {
                                        String message = (String) sendUrlResult.get("message");
                                        Toast.makeText(StartActivity.this, message, Toast.LENGTH_SHORT).show();
                                    }

                                } catch (MalformedURLException e) {
                                    throw new RuntimeException(e);
                                } catch (InterruptedException e) {
                                    throw new RuntimeException(e);
                                } catch (JSONException e) {
                                    throw new RuntimeException(e);
                                }


                            }
                        })
                        .show();
            } else {
                fab.setVisibility(View.VISIBLE);
            }
        }
    }

    public void codeVerify(String phoneNumber) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.phone_verify_code_layout, null);
        builder.setView(view);

        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        EditText code = view.findViewById(R.id.code);

        builder.setTitle("Код перевіркі зі смс-повідомлення")
                .setPositiveButton("Відправити", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String urlCost = "https://m.easy-order-taxi.site/api/android/approvedPhones/" + phoneNumber + "/" + code.getText();
                        Log.d("TAG", "onClick urlCost: " + urlCost);
                        try {
                            Map sendUrlMapCost = ResultSONParser.sendURL(urlCost);
                            Log.d("TAG", "onClick sendUrlMapCost: " + sendUrlMapCost);
                            if(sendUrlMapCost.get("resp_result").equals("200")) {
                                insertRecordsUser(phoneNumber);
                                Intent intent = new Intent(StartActivity.this, MainActivity.class);
                                startActivity(intent);
//                                finish();
                            } else {
                                String message = (String) sendUrlMapCost.get("message");
                                Toast.makeText(StartActivity.this, message, Toast.LENGTH_SHORT).show();
                            }

                        } catch (MalformedURLException e) {
                            throw new RuntimeException(e);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }
                })
               .show();

    }
    private String getTaxiUrlSearch(String from, String from_number, String to, String to_number, String urlAPI) {

        // Origin of route
        String str_origin = from + "/" + from_number;

        // Destination of route
        String str_dest = to + "/" + to_number;

        StartActivity.cursorDb = StartActivity.database.query(StartActivity.TABLE_SETTINGS_INFO, null, null, null, null, null, null);
        String tarif =  StartActivity.logCursor(StartActivity.TABLE_SETTINGS_INFO).get(2);

        // Building the parameters to the web service
        String parameters = str_origin + "/" + str_dest + "/" + tarif;

        // Building the url to the web service

        String url = "https://m.easy-order-taxi.site/api/android/" + urlAPI + "/" + parameters;

        return url;
    }
    private void dialogFromTo() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this, R.style.AlertDialogTheme);
        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.from_to_layout, null);
        builder.setView(view);

        from_number = view.findViewById(R.id.from_number);
        to_number = view.findViewById(R.id.to_number);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, Odessa.street());
        AutoCompleteTextView textViewFrom = view.findViewById(R.id.text_from);
        textViewFrom.setAdapter(adapter);

        textViewFrom.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                from = String.valueOf(adapter.getItem(position));
                String url = "https://m.easy-order-taxi.site/api/android/autocompleteSearchComboHid/" + from;


                Log.d("TAG", "onClick urlCost: " + url);
                Map sendUrlMapCost = null;
                try {
                    sendUrlMapCost = ResultSONParser.sendURL(url);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                String orderCost = (String) sendUrlMapCost.get("message");
                Log.d("TAG", "onClick orderCost : " + orderCost );

                if(orderCost.equals("1")) {
                    from_number.setVisibility(View.VISIBLE);
                    from_number.requestFocus();
                }
            }
        });

        AutoCompleteTextView textViewTo = view.findViewById(R.id.text_to);
        textViewTo.setAdapter(adapter);
        textViewTo.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                to = String.valueOf(adapter.getItem(position));
                String url = "https://m.easy-order-taxi.site/api/android/autocompleteSearchComboHid/" + to;


                Log.d("TAG", "onClick urlCost: " + url);
                Map sendUrlMapCost = null;
                try {
                    sendUrlMapCost = ResultSONParser.sendURL(url);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                String orderCost = (String) sendUrlMapCost.get("message");
                Log.d("TAG", "onClick orderCost : " + orderCost );

                if(orderCost.equals("1")) {
                    to_number.setVisibility(View.VISIBLE);
                    to_number.requestFocus();
                }

            }
        });

        builder.setMessage("Сформуйте маршрут")
                .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        if(from != null ) {
                            if (to == null)        {
                                to = from; to_number.setText(from_number.getText());
                            }
                            try {

                                String urlCost = getTaxiUrlSearch(from, from_number.getText().toString(), to, to_number.getText().toString(), "costSearch");

                                Log.d("TAG", "onClick urlCost: " + urlCost);
                                Map sendUrlMapCost = CostJSONParser.sendURL(urlCost);

                                String orderCost = (String) sendUrlMapCost.get("order_cost");
                                Log.d("TAG", "onClick orderCost : " + orderCost );

                                if(!orderCost.equals("0")) {



                                    // Start downloading json data from Google Directions API

                                    new MaterialAlertDialogBuilder(StartActivity.this, R.style.AlertDialogTheme)
                                            .setMessage("Вартість поїздки: " + orderCost + "грн")
                                            .setPositiveButton("Замовити", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {

                                                    String urlOrder = getTaxiUrlSearch(from, from_number.getText().toString(), to, to_number.getText().toString(), "orderSearch");

                                                    try {
                                                        Map sendUrlMap = OrderJSONParser.sendURL(urlOrder);

                                                        String orderWeb = (String) sendUrlMap.get("order_cost");
                                                        if(!orderWeb.equals("0")) {
                                                            String from_name = (String) sendUrlMap.get("from_name");
                                                            String to_name = (String) sendUrlMap.get("to_name");
                                                            if (from_name.equals(to_name)) {
                                                                messageResult = "Дякуемо за замовлення зі " +
                                                                        from_name + " " + from_number.getText() +  " " + " по місту." +
                                                                        " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";

                                                            } else {
                                                                messageResult = "Дякуемо за замовлення зі " +
                                                                        from_name + " " + from_number.getText() +  " " + " до " +
                                                                        to_name + " " +  to_number.getText() +  "." +
                                                                        " Очикуйте дзвонка оператора. Вартість поїздки: " + orderWeb + "грн";
                                                            }

                                                            new MaterialAlertDialogBuilder(StartActivity.this, R.style.AlertDialogTheme)
                                                                    .setMessage(messageResult)
                                                                    .setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                            Log.d("TAG", "onClick ");

//                                                                            Intent intent = new Intent(this, StartActivity.class);
//                                                                            startActivity(intent);
                                                                            Toast.makeText(StartActivity.this, "До побачення. Чекаємо наступного разу.", Toast.LENGTH_SHORT).show();

                                                                        }
                                                                    })
                                                                    .show();
                                                        } else {
                                                            String message = (String) sendUrlMap.get("message");
                                                            new MaterialAlertDialogBuilder(StartActivity.this, R.style.AlertDialogTheme)
                                                                    .setMessage(message +
                                                                            ". Спробуйте ще або зателефонуйте оператору.")
                                                                    .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                            Intent intent = new Intent(Intent.ACTION_CALL);
                                                                            intent.setData(Uri.parse("tel:0934066749"));
                                                                            if (ActivityCompat.checkSelfPermission(StartActivity.this,
                                                                                    Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                                                checkPermission(Manifest.permission.CALL_PHONE, StartActivity.READ_CALL_PHONE);

                                                                            }
                                                                            startActivity(intent);
                                                                        }
                                                                    })
                                                                    .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                                        @Override
                                                                        public void onClick(DialogInterface dialog, int which) {
                                                                            Intent intent = new Intent(StartActivity.this, StartActivity.class);
                                                                            startActivity(intent);
                                                                        }
                                                                    })
                                                                    .show();
                                                        }


                                                    } catch (MalformedURLException e) {
                                                        throw new RuntimeException(e);
                                                    } catch (InterruptedException e) {
                                                        throw new RuntimeException(e);
                                                    } catch (JSONException e) {
                                                        throw new RuntimeException(e);
                                                    }
                                                }
                                            })
                                            .setNegativeButton("Відміна", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Log.d("TAG", "onClick: " + "Відміна");
                                                }
                                            })
                                            .show();
                                }
                                else {

                                    String message = (String) sendUrlMapCost.get("message");
                                    new MaterialAlertDialogBuilder(StartActivity.this, R.style.AlertDialogTheme)
                                            .setMessage(message +
                                                    ". Спробуйте ще або зателефонуйте оператору.")
                                            .setPositiveButton("Підтримка", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent(Intent.ACTION_CALL);
                                                    intent.setData(Uri.parse("tel:0934066749"));
                                                    if (ActivityCompat.checkSelfPermission(StartActivity.this,
                                                            Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                                        checkPermission(Manifest.permission.CALL_PHONE, StartActivity.READ_CALL_PHONE);
                                                    }
                                                    startActivity(intent);
                                                }
                                            })
                                            .setNegativeButton("Спробуйте ще", new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Intent intent = new Intent(StartActivity.this, StartActivity.class);
                                                    startActivity(intent);
                                                }
                                            })
                                            .show();
                                }

                            } catch (MalformedURLException e) {
                                throw new RuntimeException(e);
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            Intent intent = new Intent(StartActivity.this, StartActivity.class);
                            startActivity(intent);
                            Toast.makeText(StartActivity.this, "Вкажить місце відправлення", Toast.LENGTH_SHORT).show();
                        }


                    }
                })
                .setNegativeButton("Вхід", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(StartActivity.this, MainActivity.class);
                        startActivity(intent);
                    }
                })
                .show();
    }


}
