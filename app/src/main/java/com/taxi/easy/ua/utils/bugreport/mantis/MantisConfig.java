package com.taxi.easy.ua.utils.bugreport.mantis;

import androidx.annotation.NonNull;

public class MantisConfig {

    public final String apiToken;
    public final String baseUrl;
    public final int projectId;
    public final int categoryId;

    public MantisConfig(@NonNull String apiToken,
                        @NonNull String baseUrl,
                        int projectId,
                        int categoryId) {
        this.apiToken = apiToken;
        this.baseUrl = baseUrl;
        this.projectId = projectId;
        this.categoryId = categoryId;
    }
}
