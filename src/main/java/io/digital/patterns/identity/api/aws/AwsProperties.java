package io.digital.patterns.identity.api.aws;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties(prefix = "aws")
@Component
@Data
public class AwsProperties {

    private String region;
    private String bucketName;
    private Credentials credentials;
    private String cscaMasterListBucketName;

    @Data
    public static class Credentials {
        private String accessKey;
        private String secretKey;
    }
}
