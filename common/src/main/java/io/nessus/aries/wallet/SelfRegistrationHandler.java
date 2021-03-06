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
package io.nessus.aries.wallet;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.hyperledger.acy_py.generated.model.DID;
import org.hyperledger.aries.api.exception.AriesException;
import org.hyperledger.aries.api.ledger.IndyLedgerRoles;
import org.hyperledger.aries.config.GsonConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import io.nessus.aries.HttpClientFactory;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SelfRegistrationHandler {

    static final Logger log = LoggerFactory.getLogger(SelfRegistrationHandler.class);
    static final Gson gson = GsonConfig.defaultConfig();

    final String networkURL;

    public SelfRegistrationHandler(String url) {
        this.networkURL = url;
    }

    public boolean registerWithDID(String alias, String did, String verkey, IndyLedgerRoles role) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("did", did);
        json.addProperty("verkey", verkey);
        if (alias != null)
            json.addProperty("alias", alias);
        if (role != null)
            json.addProperty("role", role.toString());
        log.info("Self register: {}", json);
        String res = call(buildPost(json));
        json = gson.fromJson(res, JsonObject.class);
        log.info("Respose: {}", json);
        return true;
    }

    public DID registerWithSeed(String alias, String seed, IndyLedgerRoles role) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("seed", seed);
        if (alias != null)
            json.addProperty("alias", alias);
        if (role != null)
            json.addProperty("role", role.toString());
        log.info("Self register: {}", json);
        String res = call(buildPost(json));
        DID did = gson.fromJson(res, DID.class);
        log.info("Respose: {}", did);
        return did;
    }
    
    private Request buildPost(Object body) {
        MediaType JSON_TYPE = MediaType.get("application/json; charset=utf-8");
        RequestBody jsonBody = RequestBody.create(gson.toJson(body), JSON_TYPE);
        return new Request.Builder().url(networkURL).post(jsonBody).build();
    }

    private String call(Request req) throws IOException {
        String result = null;
        OkHttpClient httpClient = HttpClientFactory.createHttpClient();
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
