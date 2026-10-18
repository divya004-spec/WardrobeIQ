package com.example.wardrobeiq.network;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // TODO: put YOUR Render URL here. Must start with https:// and end with /
    // Example: "https://wardrobeiq-backend.onrender.com/"
    private static final String BASE_URL =
            "https://wardrobeiqbackend.onrender.com/";

    private static Retrofit retrofit;

    public static Retrofit getRetrofit() {

        if (retrofit == null) {

            // Long timeouts: Render's free plan sleeps and needs
            // up to ~60 seconds to wake up, and AI analysis takes a few seconds.
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(90, TimeUnit.SECONDS)
                    .writeTimeout(90, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }

        return retrofit;
    }

    public static ClothingApiService getApiService() {
        return getRetrofit().create(ClothingApiService.class);
    }
}