package com.taxi.easy.ua.ui.gallery;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentGalleryBinding;
import com.taxi.easy.ua.ui.start.StartActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GalleryFragment extends Fragment {

    private FragmentGalleryBinding binding;
    private ListView listView;
    private String[] array;
    private static final int CM_DELETE_ID = 1;
    TextView textView;
    Button button;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        GalleryViewModel galleryViewModel =
                new ViewModelProvider(this).get(GalleryViewModel.class);

        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        textView = binding.textGallery;
        textView.setText(R.string.my_routs);

        listView = root.findViewById(R.id.listView);

        array = arrayToRoutsAdapter ();
        if(array != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.services_adapter_layout, array);
            listView.setAdapter(adapter);
            listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

            registerForContextMenu(listView);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                     button = binding.delBut;
                     button.setVisibility(View.VISIBLE);
                    button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            deleteRouts();
                        }
                    });

                }
            });
        } else {
            textView.setText(R.string.no_routs);
        }
        return root;
    }

    private void reIndexOrders() {
        SQLiteDatabase database = getActivity().openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
        database.execSQL("CREATE TABLE  temp_table" + "(id integer primary key autoincrement," +
                " from_street text," +
                " from_number text," +
                " from_lat text," +
                " from_lng text," +
                " to_street text," +
                " to_number text," +
                " to_lat text," +
                " to_lng text);");
        // Копирование данных из старой таблицы во временную
        database.execSQL("INSERT INTO temp_table SELECT * FROM " + StartActivity.TABLE_ORDERS_INFO);

        // Удаление старой таблицы
        database.execSQL("DROP TABLE " + StartActivity.TABLE_ORDERS_INFO);

        // Создание новой таблицы
        database.execSQL("CREATE TABLE " + StartActivity.TABLE_ORDERS_INFO + "(id integer primary key autoincrement," +
                " from_street text," +
                " from_number text," +
                " from_lat text," +
                " from_lng text," +
                " to_street text," +
                " to_number text," +
                " to_lat text," +
                " to_lng text);");

        String query = "INSERT INTO " + StartActivity.TABLE_ORDERS_INFO + " (from_street, from_number, from_lat, from_lng, to_street, to_number, to_lat, to_lng) " +
                "SELECT from_street, from_number, from_lat, from_lng, to_street, to_number, to_lat, to_lng FROM temp_table";

        // Копирование данных из временной таблицы в новую
        database.execSQL(query);

        // Удаление временной таблицы
        database.execSQL("DROP TABLE temp_table");
        database.close();
    }



    private void deleteRouts () {
        SparseBooleanArray checkedPositions = listView.getCheckedItemPositions();
        ArrayList<Integer> selectedPositions = new ArrayList<>();

        for (int i = 0; i < checkedPositions.size(); i++) {
            int pos = checkedPositions.keyAt(i);
            if (checkedPositions.get(pos)) {
                selectedPositions.add(pos);
            }
        }
//        reIndexOrders();
        for (int position : selectedPositions) {
            int i = position + 1;

            String deleteQuery = "DELETE FROM " + StartActivity.TABLE_ORDERS_INFO + " WHERE id = " + i  + ";";
            SQLiteDatabase database = getActivity().openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);

            database.execSQL(deleteQuery);
            database.close();
        }
        reIndexOrders();
        array = arrayToRoutsAdapter();
        if (array != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.services_adapter_layout, array);
            listView.setAdapter(adapter);
        } else {
            // Если массив пустой, отобразите текст "no_routs" вместо списка
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.services_adapter_layout, new String[]{});
            listView.setAdapter(adapter);
            textView.setText(R.string.no_routs);
            button.setVisibility(View.INVISIBLE);
        }


    }

    private String[] arrayToRoutsAdapter() {
        ArrayList<Map> routMaps = routMaps(getContext());
        String[] arrayRouts;
        if(routMaps.size() != 0) {
            arrayRouts = new String[routMaps.size()];
            for (int i = 0; i < routMaps.size(); i++) {
                if(!routMaps.get(i).get("from_street").toString().equals(routMaps.get(i).get("to_street").toString())) {
                    if (!routMaps.get(i).get("from_street").toString().equals(routMaps.get(i).get("from_number").toString())) {
                        arrayRouts[i] = routMaps.get(i).get("from_street").toString() + " " +
                                routMaps.get(i).get("from_number").toString() + " -> " +
                                routMaps.get(i).get("to_street").toString() + " " +
                                routMaps.get(i).get("to_number").toString();
                    } else if(!routMaps.get(i).get("to_street").toString().equals(routMaps.get(i).get("to_number").toString())) {
                        arrayRouts[i] = routMaps.get(i).get("from_street").toString() +
                                getString(R.string.to_message) +
                                routMaps.get(i).get("to_street").toString() + " " +
                                routMaps.get(i).get("to_number").toString();
                    } else {
                        arrayRouts[i] = routMaps.get(i).get("from_street").toString()  +
                                getString(R.string.to_message) +
                                routMaps.get(i).get("to_street").toString();

                    }

                } else {
                    arrayRouts[i] = routMaps.get(i).get("from_street").toString() + " " +
                            routMaps.get(i).get("from_number").toString() + " -> " +
                            getString(R.string.on_city_tv);
                }

            }
        } else {
            arrayRouts = null;
        }
        return arrayRouts;
    }

    private ArrayList<Map> routMaps(Context context) {
        Map <String, String> routs;
        ArrayList<Map> routsArr = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(StartActivity.DB_NAME, MODE_PRIVATE, null);
        Cursor c = database.query(StartActivity.TABLE_ORDERS_INFO, null, null, null, null, null, null);
        int i = 0;
        if (c != null) {
            if (c.moveToFirst()) {
                do {
                    routs = new HashMap<>();
                    routs.put("id", c.getString(c.getColumnIndexOrThrow ("id")));
                    routs.put("from_street", c.getString(c.getColumnIndexOrThrow ("from_street")));
                    routs.put("from_number", c.getString(c.getColumnIndexOrThrow ("from_number")));
                    routs.put("to_street", c.getString(c.getColumnIndexOrThrow ("to_street")));
                    routs.put("to_number", c.getString(c.getColumnIndexOrThrow ("to_number")));
                    routsArr.add(i++, routs);
                } while (c.moveToNext());
            }
        }
        database.close();
        Log.d("TAG", "routMaps: " + routsArr);
        return routsArr;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}