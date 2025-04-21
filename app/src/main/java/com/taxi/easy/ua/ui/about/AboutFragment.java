package com.taxi.easy.ua.ui.about;

import static com.taxi.easy.ua.androidx.startup.MyApplication.sharedPreferencesHelperMain;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentAboutBinding;
import com.uxcam.UXCam;

public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;
    private int desiredHeight;

    String TAG = "AboutFragment";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

        AboutViewModel aboutViewModel =
                new ViewModelProvider(this).get(AboutViewModel.class);

        binding = FragmentAboutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Текст, который вы хотите отображать
        TextView textSite = binding.textView1;
        // Текст, который вы хотите отображать
        String displayText = requireActivity().getString(R.string.gdpr0);

        String baseUrl = (String) sharedPreferencesHelperMain.getValue("baseUrl", "https://m.easy-order-taxi.site");
        String url = baseUrl +"/taxi-gdbr";

        SpannableString spannableString = new SpannableString(displayText);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Обработка нажатия на ссылку
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        };
        spannableString.setSpan(clickableSpan, 0, displayText.length(), SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE);
        textSite.setText(spannableString);
        textSite.setMovementMethod(LinkMovementMethod.getInstance());

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("IntentReset")
            @Override
            public void onClick(View view) {
                String subject = getString(R.string.android);
                String body = getString(R.string.good_day);

                String[] CC = {""};
                Intent emailIntent = new Intent(Intent.ACTION_SEND);

                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.setType("text/plain");
                emailIntent.putExtra(Intent.EXTRA_CC, CC);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);

                try {
                    startActivity(Intent.createChooser(emailIntent, getString(R.string.share)));
                } catch (android.content.ActivityNotFoundException e) {
                    FirebaseCrashlytics.getInstance().recordException(e);
                }

            }
        });


        final TextView textViewBuild = binding.textBuild;
        final TextView textViewEmail = binding.textEmail;
        aboutViewModel.getTextBuild().observe(getViewLifecycleOwner(), textViewBuild::setText);
        aboutViewModel.getTextEmail().observe(getViewLifecycleOwner(), textViewEmail::setText);
        ScrollView scrollView = binding.layoutScroll;




        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                // Теперь мы можем получить высоту фрагмента
                desiredHeight = root.getHeight() - 350;
                ViewGroup.LayoutParams layoutParams = scrollView.getLayoutParams();
                layoutParams.height = desiredHeight;
                scrollView.setLayoutParams(layoutParams);
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}