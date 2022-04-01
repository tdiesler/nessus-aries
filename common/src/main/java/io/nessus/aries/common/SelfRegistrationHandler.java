package io.nessus.aries.common;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.ledger.IndyLedgerRoles;
import org.hyperledger.aries.config.GsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SelfRegistrationHandler {

    static final Logger log = LoggerFactory.getLogger(SelfRegistrationHandler.class);
    static final Gson gson = GsonConfig.defaultConfig();

    final String networkURL;
    final OkHttpClient httpClient;

    public SelfRegistrationHandler(String url) {
        this.networkURL = url;
        this.httpClient = new OkHttpClient.Builder()
                .writeTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
                .callTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    public boolean registerDID(String did, String verkey, IndyLedgerRoles role) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("did", did);
        json.addProperty("verkey", verkey);
        if (role != null)
            json.addProperty("role", role.toString());
        log.info("Self register: {}", json);
        String res = call(buildPost(json));
        gson.fromJson(res, JsonObject.class);
        return true;
    }

    private Request buildPost(Object body) {
        MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
        RequestBody jsonBody = RequestBody.create(gson.toJson(body), JSON_TYPE);
        return new Request.Builder().url(networkURL).post(jsonBody).build();
    }

    private String call(Request req) throws IOException {
        String result = null;
        try (Response resp = httpClient.newCall(req).execute()) {
            if (resp.isSuccessful() && resp.body() != null) {
                result = resp.body().string();
            } else if (!resp.isSuccessful()) {
                handleError(resp);
            }
        }
        return result;
    }

    private void handleError(Response resp) throws IOException {
        String msg = StringUtils.isNotEmpty(resp.message()) ? resp.message() : "";
        String body = resp.body() != null ? resp.body().string() : "";
        log.error("code={} message={}\nbody={}", resp.code(), msg, body);
        throw new AriesException(resp.code(), msg + "\n" + body);
    }
}