package com.taxi.easy.ua.ui.card;

import static android.content.Context.MODE_PRIVATE;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.card.unlink.UnlinkApi;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CustomCardAdapter extends ArrayAdapter<Map<String, String>> {
    private static final String TAG = "CustomCardAdapter";
    private final ArrayList<Map<String, String>> cardMaps;
    private int selectedPosition = 0;
    public static String rectoken;
    public static String table;
    public static String pay_method;

//    private  String baseUrl = "https://m.easy-order-taxi.site";
    private  final String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
    public CustomCardAdapter(Context context, ArrayList<Map<String, String>> cardMaps, String table, String pay_method) {
        super(context, R.layout.cards_adapter_layout, cardMaps);
        this.cardMaps = cardMaps;
        CustomCardAdapter.table = table;
        CustomCardAdapter.pay_method = pay_method;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        ViewHolder holder;

        if (view == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            view = inflater.inflate(R.layout.cards_adapter_layout, parent, false);
            holder = new ViewHolder();

            holder.cardImage = view.findViewById(R.id.cardImage); // Правильное определение cardImage
            holder.cardText = view.findViewById(R.id.cardText);
            holder.bankText = view.findViewById(R.id.bankText);
            holder.deleteButton = view.findViewById(R.id.deleteButton);
            holder.checkBox = view.findViewById(R.id.checkBox);

            view.setTag(holder);
        } else {
            holder = (ViewHolder) view.getTag();
        }

        final Map<String, String> cardMap = getItem(position);

        if (cardMap != null) {
            String cardType = cardMap.get("card_type");
            if ("VISA".equals(cardType)) {
                holder.cardImage.setImageResource(R.drawable.visa); // Имя изображения для VISA
            } else if ("MasterCard".equals(cardType)) {
                holder.cardImage.setImageResource(R.drawable.mastercard); // Имя изображения для MasterCard
            } else {
                holder.cardImage.setImageResource(R.drawable.default_card); // Имя изображения по умолчанию
            }
            String masked_card = cardMap.get("masked_card");
            holder.cardText.setText(masked_card);

            String bank_name = cardMap.get("bank_name");
            Logger.d(getContext(), TAG, "getView:11111 bank_name" +  bank_name);

            assert bank_name != null;
            if(!bank_name.equals("SOME BANK IN UA COUNTRY")) {
                holder.bankText.setText(bank_name);
            } else {
                holder.bankText.setText("BANK IN UA");
            }


            selectedPosition = getCheckRectoken(table);
            Logger.d(getContext(), TAG, "getView: " + selectedPosition);
            holder.checkBox.setChecked(position == selectedPosition);
            rectoken = cardMap.get("rectoken");

//            notifyDataSetChanged();

            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedPosition = position;


                    rectoken = cardMap.get("rectoken");
                    Logger.d(getContext(), TAG, "onClick:rectoken " + rectoken);
                    updateRectokenCheck(table,  rectoken);
                    notifyDataSetChanged();
                }
            });
            // Обработчик для кнопки удаления
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Получите позицию, которую нужно удалить
                    int position = cardMaps.indexOf(cardMap);

                    String rectoken = cardMap.get("rectoken");
                    // Удалите элемент из базы данных
                    SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                    switch (pay_method) {
                        case "wfp_payment":

                            deleteCardToken(rectoken);
                            break;
                        case "fondy_payment":
                            deleteCardToken(rectoken);
                            break;

                    }
                    database.close();



                    // Удалите элемент из cardMaps
                    if (position >= 0) {
                        cardMaps.remove(position);
                        notifyDataSetChanged(); // Обновите адаптер после удаления
                    }
                }
            });
        }

        return view;
    }

    @SuppressLint("Range")
    private Pair<Integer, Integer> getMinMaxId(String table) {
        SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        String[] columns = {"MIN(id) AS min_id", "MAX(id) AS max_id"};
        Cursor cursor = database.query(table, columns, null, null, null, null, null);

        int minId = -1;
        int maxId = -1;

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                minId = cursor.getInt(cursor.getColumnIndex("min_id"));
                maxId = cursor.getInt(cursor.getColumnIndex("max_id"));
            }
            cursor.close();
        }

        database.close();
        return new Pair<>(minId, maxId);
    }

    private int getCheckRectoken(String table) {
        Pair<Integer, Integer> minMaxId = getMinMaxId(table);
        int minId = minMaxId.first;
        int maxId = minMaxId.second;

        // Проверяем случаи, когда таблица пуста или не содержит строк с rectoken_check = 1
        if (minId == -1 || maxId == -1) {
            return -1; // Возвращаем -1, чтобы указать на отсутствие строки
        }

        SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        String[] columns = {"id"};
        String selection = "rectoken_check = ? AND id >= ? AND id <= ?";
        String[] selectionArgs = {"1", String.valueOf(minId), String.valueOf(maxId)};
        Cursor cursor = database.query(table, columns, selection, selectionArgs, null, null, null);

        int position = -1; // Инициализируем позицию -1, чтобы обозначить, что строка не найдена

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                int idColumnIndex = cursor.getColumnIndex("id");
                int id = cursor.getInt(idColumnIndex);
                // Вычисляем позицию строки в списке
                position = id - minId;
            }
            cursor.close();
        }
        Logger.d(getContext(), TAG, "getCheckRectokenPosition: position" + position);
        database.close();
        return position;
    }



    private void updateRectokenCheck(String table, String rectoken) {
        SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        // Устанавливаем -1 для всех записей
        ContentValues allOtherValues = new ContentValues();
        allOtherValues.put("rectoken_check", -1);

        database.update(table, allOtherValues, null, null);

        // Устанавливаем 1 для целевой записи
        ContentValues targetValue = new ContentValues();
        targetValue.put("rectoken_check", 1);

        String whereClause = "rectoken = ?";
        String[] whereArgs = {rectoken};

        int rowsUpdated = database.update(table, targetValue, whereClause, whereArgs);

        if (rowsUpdated > 0) {
            Logger.d(getContext(), TAG, "Updated rectoken_check for rectoken " + rectoken + " to 1");
        } else {
            Logger.d(getContext(), TAG, "No rows were updated. " + rectoken + " may not exist.");
        }

        database.close();
    }


    public void deleteCardToken(String rectoken) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        UnlinkApi apiService = retrofit.create(UnlinkApi.class);
        Call<Void> call = apiService.deleteCardTokenFondy(rectoken);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    // Обработка успешного ответа

                    Toast.makeText(getContext(), getContext().getString(R.string.un_link_token), Toast.LENGTH_LONG).show();
                    SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                    database.delete(MainActivity.TABLE_WFP_CARDS, "rectoken = ?", new String[]{rectoken});
                    database.close();
                    reIndexCards();

                } else {
//                    Toast.makeText(getContext(), getContext().getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), getContext().getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
            }
        });
    }
    private void reIndexCards() {
        SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        // Проверяем, существует ли таблица temp_table, и если она существует, удаляем её
        if (isTableExists(database, "temp_table")) {
            database.execSQL("DROP TABLE IF EXISTS temp_table");
        }

        // Создаем таблицу temp_table
        database.execSQL("CREATE TABLE temp_table (id integer primary key autoincrement," +
                " masked_card text," +
                " card_type text," +
                " bank_name text," +
                " rectoken text," +
                " merchant text," +
                " rectoken_check text);");

        // Копирование данных из старой таблицы во временную
        database.execSQL("INSERT INTO temp_table SELECT * FROM " + table);

        // Удаление старой таблицы
        database.execSQL("DROP TABLE IF EXISTS " + table);

        // Создание новой таблицы
        database.execSQL("CREATE TABLE " + table + "(id integer primary key autoincrement," +
                " masked_card text," +
                " card_type text," +
                " bank_name text," +
                " rectoken text," +
                " merchant text," +
                " rectoken_check text);");

        // Копирование данных из временной таблицы в новую
        database.execSQL("INSERT INTO " + table + " (masked_card, card_type, bank_name, rectoken, merchant, rectoken_check) " +
                "SELECT masked_card, card_type, bank_name, rectoken, merchant, rectoken_check FROM temp_table");

        // Удаление временной таблицы
        database.execSQL("DROP TABLE IF EXISTS temp_table");
        database.close();

        ArrayList<Map<String, String>> cardMaps = getCardMapsFromDatabase();
        Logger.d(getContext(), TAG, "onResume: cardMaps" + cardMaps);

        if (cardMaps.isEmpty()) {
            // Если массив пустой, отобразите текст "no_routs" вместо списка
            CardFragment.textCard.setVisibility(View.VISIBLE);
            CardFragment.listView.setVisibility(View.GONE);
            CardFragment.textCard.setText(R.string.no_cards);
        }
    }


    // Проверка наличия таблицы в базе данных
    private boolean isTableExists(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name=?", new String[]{tableName});
        boolean tableExists = cursor.moveToFirst();
        cursor.close();
        return tableExists;
    }


    @SuppressLint("Range")
    private ArrayList<Map<String, String>> getCardMapsFromDatabase() {
        ArrayList<Map<String, String>> cardMaps = new ArrayList<>();
        SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        // Выполните запрос к таблице TABLE_FONDY_CARDS и получите данные
        Cursor cursor = database.query(table, null, null, null, null, null, null);
        Logger.d(getContext(), TAG, "getCardMapsFromDatabase: card count: " + cursor.getCount());

        if (cursor.moveToFirst()) {
            do {
                Map<String, String> cardMap = new HashMap<>();
                cardMap.put("card_type", cursor.getString(cursor.getColumnIndex("card_type")));
                cardMap.put("bank_name", cursor.getString(cursor.getColumnIndex("bank_name")));
                cardMap.put("masked_card", cursor.getString(cursor.getColumnIndex("masked_card")));
                cardMap.put("rectoken", cursor.getString(cursor.getColumnIndex("rectoken")));

                cardMaps.add(cardMap);
            } while (cursor.moveToNext());
        }
        cursor.close();
        database.close();

        return cardMaps;
    }



    static class ViewHolder {
        ImageView cardImage;
        TextView cardText, bankText;
        ImageView deleteButton;
        CheckBox checkBox;
    }

}
