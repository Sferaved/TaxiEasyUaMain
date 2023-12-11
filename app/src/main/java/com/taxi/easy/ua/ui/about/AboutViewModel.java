package com.taxi.easy.ua.ui.about;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.taxi.easy.ua.R;

public class AboutViewModel extends ViewModel {


    private final MutableLiveData<String> mText_author = new MutableLiveData<>();
    private final MutableLiveData<String> mText_email = new MutableLiveData<>();
    private final MutableLiveData<String> mText_build = new MutableLiveData<>();

    public AboutViewModel() {
        mText_email.setValue("Email: taxi.easy.ua@gmail.com");
        mText_build.setValue("2023");
    }


    public LiveData<String> getTextAuthor() {
        return mText_author;
    }

    public LiveData<String> getTextEmail() {
        return mText_email;
    }
    public LiveData<String> getTextBuild() {
        return mText_build;
    }

}