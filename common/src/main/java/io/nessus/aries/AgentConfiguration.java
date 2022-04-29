package io.nessus.aries;

import java.net.MalformedURLException;
import java.net.URL;

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
        return AgentConfiguration.builder()
                .adminUrl("http://localhost:8031")
                .userUrl("http://localhost:8030")
                .apiKey("adminkey")
                .build();
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