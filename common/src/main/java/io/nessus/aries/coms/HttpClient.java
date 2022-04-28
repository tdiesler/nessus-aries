package io.nessus.aries.coms;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.config.GsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;

public final class HttpClient {
    
    static final Logger log = LoggerFactory.getLogger(HttpClient.class);
    
    // Hide ctor
    private HttpClient() {}
    
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
                    try {
                        Object json = gson.fromJson(msg, Object.class);
                        log.trace("\n{}", pretty.toJson(json));
                    } catch (JsonSyntaxException e) {
                        log.trace("{}", msg);
                    }
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