package com.taxi.easy.ua.ui.home;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatButton;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;


public class MyBottomSheetCityFragment extends BottomSheetDialogFragment {

    ListView listView;
    String city;
    AppCompatButton btn_ok;

    public MyBottomSheetCityFragment(String city) {
        this.city = city;
    }
    private final String[] cityList = new String[]{
            "Київ",
            "Дніпро",
            "Одеса",
            "Запоріжжя",
            "Черкаси",

    };
    private final String[] cityCode = new String[]{
            "Kyiv City",
            "Dnipropetrovsk Oblast",
            "Odessa",
            "Zaporizhzhia",
            "Cherkasy Oblast",

    };

    int positionFirst;

    @SuppressLint("MissingInflatedId")
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bonus_list_layout, container, false);
        listView = view.findViewById(R.id.listViewBonus);
        HomeFragment.progressBar.setVisibility(View.INVISIBLE);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), R.layout.services_adapter_layout, cityList);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);


        switch (this.city){
            case "Kyiv City":
                positionFirst = 0;
                break;
            case "Dnipropetrovsk Oblast":
                positionFirst = 1;
                break;
            case "Odessa":
                positionFirst = 2;
                break;
            case "Zaporizhzhia":
                positionFirst = 3;
                break;
            case "Cherkasy Oblast":
                positionFirst = 4;
                break;
            default:
                positionFirst = 0;
                break;
        }
        listView.setItemChecked(positionFirst,true);
        int positionFirstOld = positionFirst;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                positionFirst = position;
            }
        });

        btn_ok = view.findViewById(R.id.btn_ok);
        btn_ok.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(positionFirstOld != positionFirst){
                    ContentValues cv = new ContentValues();
                    SQLiteDatabase database = view.getContext().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

                    cv.put("city", cityCode [positionFirst]);
                    database.update(MainActivity.CITY_INFO, cv, "id = ?",
                            new String[] { "1" });
                    database.close();

                    NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
                    resetRoutHome();
                    navController.navigate(R.id.nav_home);

                    Toast.makeText(getActivity(), getString(R.string.change_message) + cityList [positionFirst]   , Toast.LENGTH_SHORT).show();

                }
               dismiss();
            }
        });
        return view;
    }
    public void resetRoutHome() {
        ContentValues cv = new ContentValues();

        cv.put("from_street", " ");
        cv.put("from_number", " ");
        cv.put("to_street", " ");
        cv.put("to_number", " ");

        // обновляем по id
        SQLiteDatabase database = requireActivity().openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
        database.update(MainActivity.ROUT_HOME, cv, "id = ?",
                new String[] { "1" });
        database.close();
    }

   }

