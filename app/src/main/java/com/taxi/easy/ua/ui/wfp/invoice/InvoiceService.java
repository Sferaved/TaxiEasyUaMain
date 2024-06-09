package com.taxi.easy.ua.ui.wfp.invoice;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface InvoiceService {
    @GET("/wfp/createInvoice/{application}/{city}/{orderReference}/{amount}/{language}/{productName}/{clientEmail}/{clientPhone}")
    Call<InvoiceResponse> createInvoice(
            @Path("application") String application,
            @Path("city") String city,
            @Path("orderReference") String orderReference,
            @Path("amount") int amount,
            @Path("language") String language,
            @Path("productName") String productName,
            @Path("clientEmail") String clientEmail,
            @Path("clientPhone") String clientPhone
    );
}
