package com.taxi.easy.ua.ui.author;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.time.LocalDate;

public class AuthorViewModel extends ViewModel {


    private final MutableLiveData<String> mText_author = new MutableLiveData<>();
    private final MutableLiveData<String> mText_email = new MutableLiveData<>();
    private final MutableLiveData<String> mText_build = new MutableLiveData<>();


    public AuthorViewModel() {
        mText_email.setValue("taxi.easy.ua@gmail.com");
        LocalDate currentDate = LocalDate.now();
        int currentYear = currentDate.getYear();

        mText_build.setValue(String.valueOf(currentYear));
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