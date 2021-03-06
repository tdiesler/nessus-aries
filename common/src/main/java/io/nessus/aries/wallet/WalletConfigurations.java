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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

public class WalletConfigurations {

    static final Logger log = LoggerFactory.getLogger(WalletConfigurations.class);

    private Map<String, WalletConfig> configs = new LinkedHashMap<>();

    // Hide ctor
    private WalletConfigurations() {
    }
    
    @SuppressWarnings("unchecked")
    public static WalletConfigurations loadWalletConfigurations() throws IOException {
        String home = System.getProperty("user.home");
        File infile = new File(home + "/.config/aries-cli/config.yaml");
        if (!infile.isFile())
            throw new FileNotFoundException(infile.getAbsolutePath());
        
        InputStream fis = new FileInputStream(infile);
        Yaml yaml = new Yaml();
        
        WalletConfigurations result = new WalletConfigurations();
        
        Map<String, Object> loaded = yaml.load(fis);
        loaded = (Map<String, Object>) loaded.get("configurations");
        
        for (Entry<String, Object> en : loaded.entrySet()) {
            Map<String, String> val = (Map<String, String>) en.getValue();
            result.configs.put(en.getKey().toLowerCase(), new WalletConfig(en.getKey(), 
                    val.get("endpoint"), 
                    val.get("api_key"), 
                    val.get("auth_token")));
        }
        return result;
    }
    
    public WalletConfig getWalletConfig(String name) {
        return configs.get(name.toLowerCase());
    }
    
    public static class WalletConfig {
        public final String name;
        public final String endpoint;
        public final String api_key;
        public final String auth_token;
        WalletConfig(String name, String endpoint, String api_key, String auth_token) {
            this.name = name;
            this.endpoint = endpoint;
            this.api_key = api_key;
            this.auth_token = auth_token;
        }
    }
}
