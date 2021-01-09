package com.example.clienteandroidpython.Retrofit;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface IUploadAPI {
    @Multipart
    @POST("/api/upload")
    Call<String> uploadFile(
            @Part ("selector") RequestBody selector,
            @Part MultipartBody.Part file
    );

}
