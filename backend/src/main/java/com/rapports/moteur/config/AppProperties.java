package com.rapports.moteur.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class AppProperties {
    private String storagePath = "./data/reports";
    private Storage storage = new Storage();
    private Jwt jwt = new Jwt();
    private Rendering rendering = new Rendering();

    @Data
    public static class Storage {
        private String provider = "local";
        private String localPath = "./data/reports";
        private S3 s3 = new S3();
    }

    @Data
    public static class S3 {
        private String endpoint = "http://localhost:9000";
        private String region = "us-east-1";
        private String bucket = "rapports-pdf";
        private String accessKey = "minioadmin";
        private String secretKey = "minioadmin";
        private boolean autoCreateBucket = true;
    }

    @Data
    public static class Jwt {
        private String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        private long expirationMs = 86400000L;
    }

    @Data
    public static class Rendering {
        private String engine = "gotenberg";
        private String gotenbergEndpoint = "http://localhost:3000";
        private int timeoutSeconds = 15;
    }
}