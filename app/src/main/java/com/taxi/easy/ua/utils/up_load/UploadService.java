package com.taxi.easy.ua.utils.up_load;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface UploadService {
    @Multipart
    @POST("upload-log")
    Call<UploadResponse> uploadLog(@Part MultipartBody.Part file);
}
