package com.taxi.easy.ua.utils.bottom_sheet;

import static android.content.Context.MODE_PRIVATE;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.taxi.easy.ua.MainActivity;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.ui.finish.fragm.CustomListFinishAdapter;
import com.taxi.easy.ua.ui.finish.fragm.FinishSeparateFragment;
import com.taxi.easy.ua.ui.home.CustomListAdapter;
import com.taxi.easy.ua.ui.home.HomeFragment;
import com.taxi.easy.ua.utils.cost_json_parser.CostJSONParserRetrofit;
import com.taxi.easy.ua.utils.data.DataArr;
import com.taxi.easy.ua.utils.log.Logger;

import java.util.ArrayList;
import java.util.List;


public class MyBottomSheetFinishOptionFragment extends BottomSheetDialogFragment {
    private static final String TAG = "MyBottomSheetFinishOptionFragment";
    ListView listView;
    public String[] arrayService;
    public static String[] arrayServiceCode;
    SQLiteDatabase database;
    Context context;
    String flexibleTariffName;
    String commentInfo;
    String extraChargeCodes;
    boolean options;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings_finish_layout, container, false);

        context = requireActivity();
        Bundle args = getArguments();
        if (args != null) {
            flexibleTariffName = args.getString("flexible_tariff_name");
            commentInfo = args.getString("comment_info");
            extraChargeCodes = args.getString("extra_charge_codes");

            // Используйте полученные значения в вашем фрагменте
        }
        Logger.d(context, TAG, "flexibleTariffName" + flexibleTariffName);
        Logger.d(context, TAG, "commentInfo: " + commentInfo);
        TextView textView = view.findViewById(R.id.komenterinp);

// Проверяем на null и на пустую строку с учетом возможных пробелов
        if (commentInfo != null && !commentInfo.trim().isEmpty()) {
            commentInfo = delAdminMessage(commentInfo);
            if(!commentInfo.equals("")) {
                view.findViewById(R.id.komentar).setVisibility(View.VISIBLE);
                textView.setVisibility(View.VISIBLE);
                textView.setText(commentInfo);
                options = true;
            }

        } else {
            view.findViewById(R.id.komentar).setVisibility(View.GONE);
            textView.setVisibility(View.GONE);
        }
        view.findViewById(R.id.komenterinp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        listView = view.findViewById(R.id.list);

        Button button = view.findViewById(R.id.btnOk);
        button.setOnClickListener(v -> {
            dismiss();
        });


        database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);

        Logger.d(context, TAG, "extraChargeCodes: " + extraChargeCodes);

// Массив услуг (отображаемых значений)
        arrayService = new String[]{
                getString(R.string.BAGGAGE),
                getString(R.string.ANIMAL),
                getString(R.string.CONDIT),
                getString(R.string.MEET),
                getString(R.string.COURIER),
                getString(R.string.CHECK),
                getString(R.string.BABY_SEAT),
                getString(R.string.DRIVER),
                getString(R.string.NO_SMOKE),
                getString(R.string.ENGLISH),
                getString(R.string.CABLE),
                getString(R.string.FUEL),
                getString(R.string.WIRES),
                getString(R.string.SMOKE),
        };

// Массив кодов услуг
        arrayServiceCode = new String[]{
                "BAGGAGE",
                "ANIMAL",
                "CONDIT",
                "MEET",
                "COURIER",
                "CHECK",
                "BABY_SEAT",
                "DRIVER",
                "NO_SMOKE",
                "ENGLISH",
                "CABLE",
                "FUEL",
                "WIRES",
                "SMOKE"
        };

// Разбиваем строку extraChargeCodes на отдельные коды
        // Проверяем, что строка extraChargeCodes не равна null
        String[] extraChargeCodesArray = (extraChargeCodes != null) ? extraChargeCodes.split(",") : new String[0];


// Создаем список для хранения выбранных услуг
        List<String> selectedServices = new ArrayList<>();

// Проходим по массиву кодов услуг
        for (int i = 0; i < arrayServiceCode.length; i++) {
            // Если код услуги присутствует в extraChargeCodesArray, добавляем соответствующую услугу в selectedServices
            for (String code : extraChargeCodesArray) {
                if (code.trim().equals(arrayServiceCode[i])) {
                    selectedServices.add(arrayService[i]);
                    break; // Прекращаем внутренний цикл после нахождения совпадения
                }
            }
        }

// Логируем выбранные услуги
        Logger.d(context, TAG, "Selected Services: " + selectedServices);



// Передайте только отмеченные услуги в адаптер
        if (selectedServices.size() !=0) {
            CustomListFinishAdapter adapterSet = new CustomListFinishAdapter(context, selectedServices.toArray(new String[0]), selectedServices.size());
            view.findViewById(R.id.maintext).setVisibility(View.VISIBLE);
            listView.setVisibility(View.VISIBLE);
            listView.setAdapter(adapterSet);
            options = true;
        } else {
            view.findViewById(R.id.maintext).setVisibility(View.GONE);
            view.findViewById(R.id.maintext).setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
        }


        String[] tariffArr = new String[]{
                context.getResources().getString(R.string.start_t),
                context.getResources().getString(R.string.base_onl_t),
                context.getResources().getString(R.string.base_t),
                context.getResources().getString(R.string.univers_t),
                context.getResources().getString(R.string.bisnes_t),
                context.getResources().getString(R.string.prem_t),
                context.getResources().getString(R.string.econom_t),
                context.getResources().getString(R.string.bus_t),
                context.getResources().getString(R.string.no_t),
        };
        ArrayAdapter<String> adapterTariff = new ArrayAdapter<String>(context, R.layout.my_simple_spinner_item, tariffArr);
        @SuppressLint({"MissingInflatedId", "LocalSuppress"})
        Spinner spinner = view.findViewById(R.id.list_tariff);
        spinner.setAdapter(adapterTariff);
        spinner.setPrompt("Title");
        spinner.setBackgroundResource(R.drawable.spinner_border);
        spinner.setEnabled(false); // Отключает возможность выбора
        spinner.setClickable(false); // Отключает клики


        if (flexibleTariffName != null && !flexibleTariffName.trim().isEmpty()) {
            options = true;
            String tariffOld =  flexibleTariffName;
            switch (tariffOld) {
                case "Базовий онлайн":
                    spinner.setSelection(1);
                    break;
                case "Базовый":
                    spinner.setSelection(2);
                    break;
                case "Универсал":
                    spinner.setSelection(3);
                    break;
                case "Бизнес-класс":
                    spinner.setSelection(4);
                    break;
                case "Премиум-класс":
                    spinner.setSelection(5);
                    break;
                case "Эконом-класс":
                    spinner.setSelection(6);
                    break;
                case "Микроавтобус":
                    spinner.setSelection(7);
                    break;
                default:
                    spinner.setSelection(0);
            }
        } else {
            spinner.setVisibility(View.GONE);
            view.findViewById(R.id.textView4).setVisibility(View.GONE);
        }
        Log.d(TAG, "options: " + options);
        if(!options) {
            TextView komenterinp = view.findViewById(R.id.komenterinp);
            komenterinp.setVisibility(View.VISIBLE);
            komenterinp.setText(R.string.no_options);
        }
        return view;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        int colorPressed = ContextCompat.getColor(context, R.color.colorDefault); // Цвет текста при нажатии
        int colorDefault = ContextCompat.getColor(context, R.color.colorAccent); // Исходный цвет текста

        int currentColor = FinishSeparateFragment.btn_options.getCurrentTextColor();

        if (currentColor == colorDefault) {
            FinishSeparateFragment.btn_options.setTextColor(colorPressed);
        } else {
            FinishSeparateFragment.btn_options.setTextColor(colorDefault);
        }
    }

    private String delAdminMessage(String comment) {
        // Указываем тег для логов, чтобы потом было легче найти записи
        final String TAG = "DelAdminMessage";
        Log.d(TAG, "Исходный комментарий: " + comment);

        String newComment = comment;

        // Начальная и конечная части добавленного текста
        String startMarker = "цифра номера ";
        String endMarker = ", Оплатили службе 45грн. ";

        // Находим позиции начального и конечного маркеров в строке
        int startIndex = comment.indexOf(startMarker);
        int endIndex = comment.indexOf(endMarker, startIndex);

        // Логируем индексы маркеров
        Log.d(TAG, "Позиция начального маркера: " + startIndex);
        Log.d(TAG, "Позиция конечного маркера: " + endIndex);

        // Проверяем, что маркеры найдены
        if (startIndex != -1 && endIndex != -1) {
            // Вырезаем нужный фрагмент, исключая добавленную информацию
            newComment = comment.substring(0, startIndex) + comment.substring(endIndex + endMarker.length());
            Log.d(TAG, "Комментарий после удаления добавленной информации: " + newComment);
        } else {
            Log.w(TAG, "Не удалось найти начальный или конечный маркер, возвращаем исходный комментарий.");
        }

        return newComment;
    }



    public static String[] arrayServiceCode() {
        return new String[]{
                "BAGGAGE",
                "ANIMAL",
                "CONDIT",
                "MEET",
                "COURIER",
                "CHECK_OUT",
                "BABY_SEAT",
                "DRIVER",
                "NO_SMOKE",
                "ENGLISH",
                "CABLE",
                "FUEL",
                "WIRES",
                "SMOKE",
        };
    }


    @SuppressLint("Range")
    public static List<String> logCursor(String table, Context context) {
        List<String> list = new ArrayList<>();
        SQLiteDatabase database = context.openOrCreateDatabase(MainActivity.DB_NAME, MODE_PRIVATE, null);
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
        database.close();
        return list;
    }
}

