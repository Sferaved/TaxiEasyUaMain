package com.taxi.easy.ua.ui.about;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import  com.taxi.easy.ua.R;
import  com.taxi.easy.ua.databinding.FragmentAboutBinding;

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
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(getActivity(), getString(R.string.no_email_agent), Toast.LENGTH_SHORT).show();
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