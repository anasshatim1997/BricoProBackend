package com.bricopro.config.propreties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.security.oauth2.client.registration")
public class OAuth2Properties {
    private Google google = new Google();
    private Facebook facebook = new Facebook();

    @Data
    public static class Google {
        private String clientId;
        private String clientSecret;
    }

    @Data
    public static class Facebook {
        private String clientId;
        private String clientSecret;
    }
}