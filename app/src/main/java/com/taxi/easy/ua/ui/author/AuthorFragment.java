package com.taxi.easy.ua.ui.author;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
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

import com.taxi.easy.ua.databinding.FragmentAuthorBinding;
import com.uxcam.UXCam;

public class AuthorFragment extends Fragment {

    private @NonNull FragmentAuthorBinding binding;
    private static final String TAG = "AuthorFragment";

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        UXCam.tagScreenName(TAG);

        AuthorViewModel authorViewModel =
                new ViewModelProvider(this).get(AuthorViewModel.class);

        binding = FragmentAuthorBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        TextView textSite = binding.textDev;

        // Текст, который вы хотите отображать
        String displayText = "Android Developer";

        final String url = "https://play.google.com/store/apps/dev?id=8830024160014473355";

        SpannableString spannableString = new SpannableString(displayText);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                // Обработка нажатия на ссылку
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        };
        spannableString.setSpan(clickableSpan, 0, displayText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);


        textSite.setText(spannableString);
        textSite.setMovementMethod(LinkMovementMethod.getInstance());

        final TextView textViewEmail = binding.textEmail;
        final TextView textViewBuild = binding.textBuild;


        authorViewModel.getTextEmail().observe(getViewLifecycleOwner(), textViewEmail::setText);
        authorViewModel.getTextBuild().observe(getViewLifecycleOwner(), textViewBuild::setText);

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}