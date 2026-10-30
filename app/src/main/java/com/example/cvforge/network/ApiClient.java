package com.example.cvforge.network;

import com.example.cvforge.BuildConfig;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Builds Retrofit services. One OkHttp client is shared, with a logging interceptor that
 * prints every request and response in Logcat (debug builds only).
 */
public class ApiClient {

    private static OkHttpClient httpClient;

    private ApiClient() {
    }

    /** Creates an API interface for a base address, for example https://api.github.com/. */
    public static <T> T create(String baseUrl, Class<T> apiClass) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(getHttpClient())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(apiClass);
    }

    private static OkHttpClient getHttpClient() {
        if (httpClient == null) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(BuildConfig.DEBUG
                    ? HttpLoggingInterceptor.Level.BASIC
                    : HttpLoggingInterceptor.Level.NONE);
            httpClient = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .build();
        }
        return httpClient;
    }
}
