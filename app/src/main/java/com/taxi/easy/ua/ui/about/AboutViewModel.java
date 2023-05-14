package com.taxi.easy.ua.ui.about;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class AboutViewModel extends ViewModel {

    private String version = "1.00";
    private final MutableLiveData<String> mText_version = new MutableLiveData<>();
    private final MutableLiveData<String> mText_author = new MutableLiveData<>();
    private final MutableLiveData<String> mText_site = new MutableLiveData<>();
    private final MutableLiveData<String> mText_email = new MutableLiveData<>();
    private final MutableLiveData<String> mText_build = new MutableLiveData<>();

    public AboutViewModel() {
        mText_version.setValue("Версія " + version);
        mText_author.setValue("Андрій Коржов");
        mText_site.setValue("Сайт: https://m.easy-order-taxi.site");
        mText_email.setValue("Email: taxi.easy.ua@gmail.com");
        mText_build.setValue("Зроблено: 2023 рік");
    }

    public LiveData<String> getTextVersion() {
        return mText_version;
    }
    public LiveData<String> getTextAuthor() {
        return mText_author;
    }
    public LiveData<String> getTextSite() {
        return mText_site;
    }
    public LiveData<String> getTextEmail() {
        return mText_email;
    }
    public LiveData<String> getTextBuild() {
        return mText_build;
    }

}