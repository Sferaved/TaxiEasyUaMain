package com.taxi.easy.ua.ui.card;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.card.unlink.UnlinkApi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CustomCardAdapter extends ArrayAdapter<Map<String, String>> {
    private ArrayList<Map<String, String>> cardMaps;
    private int selectedPosition = 0;
    public static String rectoken;
    public static String table;

    private String baseUrl = "https://m.easy-order-taxi.site";
    public CustomCardAdapter(Context context, ArrayList<Map<String, String>> cardMaps, String table) {
        super(context, R.layout.cards_adapter_layout, cardMaps);
        this.cardMaps = cardMaps;
        this.table = table;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
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
            holder.bankText.setText(bank_name);

            selectedPosition = getCheckRectoken(table);
            Log.d("TAG", "getView: " + selectedPosition);
            holder.checkBox.setChecked(position == selectedPosition);
            rectoken = cardMap.get("rectoken");

            notifyDataSetChanged();

            holder.checkBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedPosition = position;
                    notifyDataSetChanged();

                    rectoken = cardMap.get("rectoken");
                    updateRectokenCheck(table,  rectoken);
                }
            });
            // Обработчик для кнопки удаления
            holder.deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Получите позицию, которую нужно удалить
                    int position = cardMaps.indexOf(cardMap);

                    // Удалите элемент из базы данных

                        String rectoken = cardMap.get("rectoken");
                        SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
                        database.delete(MainActivity.TABLE_FONDY_CARDS, "rectoken = ?", new String[]{rectoken});
                        database.close();
                        deleteCardTokenFondy(rectoken);


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

    private int getCheckRectoken(String table) {
        String[] columns = {"id", "rectoken"};
        String selection = "rectoken_check = ?";
        String[] selectionArgs = {"1"};
        int result = 0;
        SQLiteDatabase database = getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor cursor = database.query(table, columns, selection, selectionArgs, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Log.d("TAG", "getCheckRectoken: cursor.getCount() " + cursor.getCount());
                if (cursor.getCount() == 1) {
                    int rectokenColumnIndex = cursor.getColumnIndex("rectoken");
                    Log.d("TAG", "getCheckRectoken: " + cursor.getString(rectokenColumnIndex));
                    updateRectokenCheck(table, cursor.getString(rectokenColumnIndex));
                } else {
                    do {
                        @SuppressLint("Range") int id = cursor.getInt(cursor.getColumnIndex("id"));
                        // В этом месте вы можете обрабатывать найденные id
                        result = id - 1;
                        Log.d("TAG", "Found id with rectoken_check = 1: " + id);
                    } while (cursor.moveToNext());
                }
            }
            cursor.close();
        }

        database.close();
        return result;

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
            Log.d("TAG", "Updated rectoken_check for rectoken " + rectoken + " to 1");
        } else {
            Log.d("TAG", "No rows were updated. " + rectoken + " may not exist.");
        }

        database.close();
    }


    public void deleteCardTokenFondy(String rectoken) {
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
                    reIndexCardsFondy();
                    Toast.makeText(getContext(), getContext().getString(R.string.un_link_token), Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), getContext().getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                Toast.makeText(getContext(), getContext().getString(R.string.verify_internet), Toast.LENGTH_LONG).show();
            }
        });
    }
    private void reIndexCardsFondy() {
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
                " rectoken_check text);");

        // Копирование данных из старой таблицы во временную
        database.execSQL("INSERT INTO temp_table SELECT * FROM " + MainActivity.TABLE_FONDY_CARDS);

        // Удаление старой таблицы
        database.execSQL("DROP TABLE IF EXISTS " + MainActivity.TABLE_FONDY_CARDS);

        // Создание новой таблицы
        database.execSQL("CREATE TABLE " + MainActivity.TABLE_FONDY_CARDS + "(id integer primary key autoincrement," +
                " masked_card text," +
                " card_type text," +
                " bank_name text," +
                " rectoken text," +
                " rectoken_check text);");

        String query = "INSERT INTO temp_table (masked_card, card_type, bank_name, rectoken, rectoken_check) " +
                "SELECT masked_card, card_type, bank_name, rectoken, rectoken_check FROM " + MainActivity.TABLE_FONDY_CARDS;


        // Копирование данных из временной таблицы в новую
        database.execSQL(query);

        // Удаление временной таблицы
        database.execSQL("DROP TABLE IF EXISTS temp_table");
        database.close();

        ArrayList<Map<String, String>> cardMaps = getCardMapsFromDatabase();
        Log.d("TAG", "onResume: cardMaps" + cardMaps);
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
        Cursor cursor = database.query(MainActivity.TABLE_FONDY_CARDS, null, null, null, null, null, null);
        Log.d("TAG", "getCardMapsFromDatabase: card count: " + cursor.getCount());

        if (cursor != null) {
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
        }
        database.close();

        return cardMaps;
    }



    static class ViewHolder {
        ImageView cardImage;
        TextView cardText, bankText;
        Button deleteButton;
        CheckBox checkBox;
    }

}
