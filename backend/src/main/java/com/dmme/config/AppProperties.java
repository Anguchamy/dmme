package com.dmme.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private Supabase supabase = new Supabase();
    private Instagram instagram = new Instagram();
    private Razorpay razorpay = new Razorpay();

    @Data
    public static class Cors {
        private String allowedOrigins = "http://localhost:5173";
    }

    @Data
    public static class Supabase {
        private String jwtSecret;
        private String projectUrl;
    }

    @Data
    public static class Instagram {
        private String appId;
        private String appSecret;
        private String verifyToken;
        private String graphBaseUrl = "https://graph.facebook.com/v21.0";

        // Instagram API with Instagram Login (OAuth) settings.
        private String redirectUri;
        private String scopes =
                "instagram_business_basic,instagram_business_manage_messages,instagram_business_manage_comments";
        private String oauthAuthorizeUrl = "https://www.instagram.com/oauth/authorize";
        private String oauthTokenUrl = "https://api.instagram.com/oauth/access_token";
        private String igGraphBaseUrl = "https://graph.instagram.com";
    }

    @Data
    public static class Razorpay {
        private String keyId;
        private String keySecret;
        private String webhookSecret;
    }
}
