package com.taxi.easy.ua.ui.about;

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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.taxi.easy.ua.R;
import com.taxi.easy.ua.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AboutViewModel aboutViewModel =
                new ViewModelProvider(this).get(AboutViewModel.class);

        binding = FragmentAboutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        TextView textSite = binding.textSite;

        // Текст, который вы хотите отображать
        String displayText = getString(R.string.my_site);

        final String url = "https://play.google.com/store/apps/dev?id=8830024160014473355";

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
                } catch (android.content.ActivityNotFoundException ignored) {

                }

            }
        });


        final TextView textViewAuthor = binding.textAuthor;
        final TextView textViewEmail = binding.textEmail;
        final TextView textViewBuild = binding.textBuild;

        aboutViewModel.getTextAuthor().observe(getViewLifecycleOwner(), textViewAuthor::setText);
        aboutViewModel.getTextEmail().observe(getViewLifecycleOwner(), textViewEmail::setText);
        aboutViewModel.getTextBuild().observe(getViewLifecycleOwner(), textViewBuild::setText);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}