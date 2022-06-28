/*-
 * #%L
 * Nessus Aries :: Common
 * %%
 * Copyright (C) 2022 Nessus
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
