package io.digital.patterns.identity.api.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import io.digital.patterns.identity.api.aws.AwsProperties;
import io.digital.patterns.identity.api.model.CSCAMasterListRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.crypto.PGPDataFormat;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.InputStream;
import java.util.Base64;
import java.util.UUID;

import static java.lang.String.format;

@Configuration
@Slf4j
public class CSCAMasterListRouteConfiguration {

    private final AmazonS3 amazonS3;
    private final AwsProperties awsProperties;

    @Value("${gpg.userId}")
    String gpgUserId;
    @Value("${gpg.password}")
    String gpgUserPassword;
    @Value("${gpg.privateKey}")
    String pgpPrivateKey;

    public CSCAMasterListRouteConfiguration(AmazonS3 amazonS3, AwsProperties awsProperties) {
        this.amazonS3 = amazonS3;
        this.awsProperties = awsProperties;
    }


    @Bean
    public PGPDataFormat pgpVerifyAndDecrypt() {
        byte[] decode = Base64.getDecoder().decode(pgpPrivateKey);
        PGPDataFormat pgpVerifyAndDecrypt = new PGPDataFormat();
        pgpVerifyAndDecrypt.setEncryptionKeyRing(decode);
        pgpVerifyAndDecrypt.setPassword(gpgUserPassword);
        pgpVerifyAndDecrypt.setKeyUserid(gpgUserId);
        return pgpVerifyAndDecrypt;
    }

    @Bean
    public RouteBuilder cscaRequestRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from(Routes.UPDATE_CSCA_MASTER_LIST_ROUTE)
                        .log("Received ${body}")
                        .process(exchange -> {
                            CSCAMasterListRequest request = exchange.getIn().getBody(CSCAMasterListRequest.class);
                            if (amazonS3.doesObjectExist(
                                    request.getBucketName(),
                                    request.getFileName()
                            )) {
                                S3Object gpgMasterList =
                                        amazonS3.getObject(request.getBucketName(), request.getFileName());
                                exchange.getIn().setBody(gpgMasterList.getObjectContent());
                            } else {
                                exchange.setException(new IllegalStateException(
                                        format("Master file could not be found in bucket %s with file name %s",
                                                request.getBucketName(), request.getFileName())
                                ));
                            }
                        }).log("Downloaded gpg master file from S3")
                        .unmarshal(pgpVerifyAndDecrypt())
                        .log("Performed decryption, preparing to upload to S3")
                        .process(exchange -> {
                            File scratchFile = File.createTempFile(UUID.randomUUID().toString(), ".txt");
                            FileUtils.copyInputStreamToFile(exchange.getIn().getBody(InputStream.class), scratchFile);
                            PutObjectResult result = amazonS3.putObject(
                                    awsProperties.getCscaMasterListBucketName(), "csca-masterlist.txt",
                                     scratchFile);
                            exchange.getIn().setBody(result.getETag());
                        }).log("Uploaded to S3 with eTag: ${body}");

            }
        };
    }

}
