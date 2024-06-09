package com.taxi.easy.ua.ui.fondy.gen_signatur;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface ApiService {
    @GET("/fondy/generateSignatureApp/{paramsString}")
    Call<SignatureResponse> generateSignature(
            @Path("paramsString")String params);
}
