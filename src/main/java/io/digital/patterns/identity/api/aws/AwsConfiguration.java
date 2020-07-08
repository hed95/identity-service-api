package io.digital.patterns.identity.api.aws;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AwsConfiguration {


    private final AwsProperties awsProperties;

    public AwsConfiguration(AwsProperties awsProperties) {
        this.awsProperties = awsProperties;
    }


    @Bean
    public AWSStaticCredentialsProvider credentials(){
        BasicAWSCredentials basicAWSCredentials =
                new BasicAWSCredentials(awsProperties.getCredentials().getAccessKey()
                , awsProperties.getCredentials().getSecretKey());
       return  new AWSStaticCredentialsProvider(basicAWSCredentials);

    }

    @Bean
    @Primary
    public AmazonS3 awsS3Client() {
        return AmazonS3ClientBuilder.standard().withRegion(Regions.fromName(awsProperties.getRegion()))
                .withCredentials(credentials()).build();
    }

}
