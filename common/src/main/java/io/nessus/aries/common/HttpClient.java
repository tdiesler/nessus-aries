package io.nessus.aries.common;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;

public final class HttpClient {
    
    // Hide ctor
    private HttpClient() {}
    
    public static OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .build();
    }
}