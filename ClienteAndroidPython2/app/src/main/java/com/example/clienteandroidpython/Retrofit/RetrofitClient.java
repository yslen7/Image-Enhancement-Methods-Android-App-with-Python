package com.example.clienteandroidpython.Retrofit;

import retrofit2.Retrofit;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofitClient=null;

    public static Retrofit getClient()
    {
        if(retrofitClient == null)
        {
            retrofitClient = new Retrofit.Builder()
//                    .baseUrl("http://10.0.2.2:5000") //10.0.2.2 es el localhost en el emulador
                    .baseUrl("http://192.168.1.72:5000") //10.0.2.2 es el localhost en el emulador
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build();
        }
        return retrofitClient;
    }
}
