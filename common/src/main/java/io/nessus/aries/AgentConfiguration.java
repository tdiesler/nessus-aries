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

import java.net.MalformedURLException;
import java.net.URL;

import io.nessus.aries.util.AssertState;

public class AgentConfiguration {
    
    private final String agentAdminUrl;
    private final String agentUserUrl;
    private final String agentApiKey;
    
    // Hide ctor
    private AgentConfiguration(String agentAdminUrl, String agentUserUrl, String agentApiKey) {
        this.agentAdminUrl = agentAdminUrl;
        this.agentUserUrl = agentUserUrl;
        this.agentApiKey = agentApiKey;
    }
    
    public static AgentConfiguration defaultConfiguration() {
        String host = getSystemEnv("ACAPY_HOSTNAME", "localhost");
        String adminPort = getSystemEnv("ACAPY_ADMIN_PORT", "8031");
        String userPort = getSystemEnv("ACAPY_USER_PORT", "8030");
        String apiKey = getSystemEnv("ACAPY_API_KEY", "adminkey");
        return AgentConfiguration.builder()
                .adminUrl(String.format("http://%s:%s", host, adminPort))
                .userUrl(String.format("http://%s:%s", host, userPort))
                .apiKey(apiKey)
                .build();
    }
    
    public static String getSystemEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        if (value == null || value.isBlank() || value.isEmpty())
            value = defaultValue;
        return value;
    }
    
    public static String assertSystemEnv(String key) {
        String value = System.getenv(key);
        AssertState.isFalse(value == null || value.isEmpty() || value.isBlank(), "Invalid " + key);
        return value;
    }
    
    public static AgentConfigurationBuilder builder() {
        return new AgentConfigurationBuilder();
    }
    
    public String getWebSocketUrl() {
        try {
            URL url = new URL(agentAdminUrl);
            return String.format("ws://%s:%d/ws", url.getHost(), url.getPort());
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public String getAdminUrl() {
        return agentAdminUrl;
    }

    public String getUserUrl() {
        return agentUserUrl;
    }

    public String getApiKey() {
        return agentApiKey;
    }

    @Override
    public String toString() {
        String reductedKey = agentApiKey != null ? agentApiKey.substring(0, 4) + "..." : null;
        return "AgentConfiguration [agentAdminUrl=" + agentAdminUrl + ", agentUserUrl=" + agentUserUrl + ", agentApiKey=" + reductedKey + "]";
    }

    public static class AgentConfigurationBuilder {
        
        private String adminUrl;
        private String userUrl;
        private String apiKey;
        
        public AgentConfigurationBuilder adminUrl(String adminUrl) {
            this.adminUrl = adminUrl;
            return this;
            
        }
        
        public AgentConfigurationBuilder userUrl(String userUrl) {
            this.userUrl = userUrl;
            return this;
            
        }
        
        public AgentConfigurationBuilder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
            
        }
        
        public AgentConfiguration build() {
            return new AgentConfiguration(adminUrl, userUrl, apiKey);
        }
    }
}
