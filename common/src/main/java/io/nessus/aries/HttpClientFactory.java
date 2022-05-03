package io.nessus.aries;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.config.GsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public final class HttpClientFactory {
    
    static final Logger log = LoggerFactory.getLogger(HttpClientFactory.class);
    
    // Hide ctor
    private HttpClientFactory() {}
    
    public static OkHttpClient createHttpClient() {
        return new OkHttpClient.Builder()
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .connectTimeout(60, TimeUnit.SECONDS)
            .callTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(defaultLoggingInterceptor())
            .build();
    }

    public static HttpLoggingInterceptor defaultLoggingInterceptor() {
        Gson gson = GsonConfig.defaultConfig();
        Gson pretty = GsonConfig.prettyPrinter();
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor(msg -> {
            if (log.isTraceEnabled() && StringUtils.isNotEmpty(msg)) {
                if (msg.startsWith("{")) {
                    Object json = gson.fromJson(msg, Object.class);
                    log.trace("\n{}", pretty.toJson(json));
                } else {
                    log.trace("{}", msg);
                }
            }
        });
        interceptor.level(HttpLoggingInterceptor.Level.BODY);
        interceptor.redactHeader("Authorization");
        interceptor.redactHeader("X-API-Key");
        return interceptor;
    }
}