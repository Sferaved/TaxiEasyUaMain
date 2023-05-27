package com.taxi.easy.ua.ui.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.taxi.easy.ua.databinding.FragmentAboutBinding;

public class AboutFragment extends Fragment {

    private FragmentAboutBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        AboutViewModel aboutViewModel =
                new ViewModelProvider(this).get(AboutViewModel.class);

        binding = FragmentAboutBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String subject = "Андроїд-додаток для швидких та дешевих поїздок по Києву та області.";
                String body = "Мої вітання. \n \n Знайшов чудовий додаток для поїздок на таксі. \n \n Раджу спробувати за посиланням в офіційному магазині Google: \n\n https://play.google.com/store/apps/details?id=com.taxieasyua.job \n\n Гарного дня. \n Ще побачимось.";

                String[] CC = {""};
                Intent emailIntent = new Intent(Intent.ACTION_SEND);

                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.setType("text/plain");
                emailIntent.putExtra(Intent.EXTRA_CC, CC);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
                emailIntent.putExtra(Intent.EXTRA_TEXT, body);

                try {
                    startActivity(Intent.createChooser(emailIntent, "Порадити другові додаток..."));
                } catch (android.content.ActivityNotFoundException ex) {
                    Snackbar.make(view, "Поштовий клієнт не встановлено.", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

            }
        });


        final TextView textViewAuthor = binding.textAuthor;
        final TextView textViewSite = binding.textSite;
        final TextView textViewEmail = binding.textEmail;
        final TextView textViewBuild = binding.textBuild;

        aboutViewModel.getTextAuthor().observe(getViewLifecycleOwner(), textViewAuthor::setText);
        aboutViewModel.getTextSite().observe(getViewLifecycleOwner(), textViewSite::setText);
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