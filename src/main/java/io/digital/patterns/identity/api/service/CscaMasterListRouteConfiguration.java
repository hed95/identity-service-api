package io.digital.patterns.identity.api.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3Object;
import io.digital.patterns.identity.api.Constants;
import io.digital.patterns.identity.api.aws.AwsProperties;
import io.digital.patterns.identity.api.model.CscaMasterListUploadRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.crypto.PGPDataFormat;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.bc.BcECContentVerifierProviderBuilder;
import org.bouncycastle.util.Store;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.security.Security;
import java.util.Base64;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import static io.digital.patterns.identity.api.Constants.CSCA_MASTER_LIST_KEY;
import static java.lang.String.format;

@Configuration
@Slf4j
public class CscaMasterListRouteConfiguration {

    private final AmazonS3 amazonS3;
    private final AwsProperties awsProperties;

    @Value("${gpg.userId}")
    String gpgUserId;
    @Value("${gpg.password}")
    String gpgUserPassword;
    @Value("${gpg.privateKey}")
    String pgpPrivateKey;
    @Value("${csCaCert}")
    String csCaCert;


    public CscaMasterListRouteConfiguration(AmazonS3 amazonS3, AwsProperties awsProperties) {
        this.amazonS3 = amazonS3;
        this.awsProperties = awsProperties;
    }


    @PostConstruct
    public void init() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        csCaCert = new String(Base64.getDecoder().decode(csCaCert));
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
    public X509CertificateHolder caCertHolder() throws Exception {
        PEMParser pemParser = new PEMParser(new StringReader(csCaCert));
        Object parsedObj = pemParser.readObject();
        return (X509CertificateHolder) parsedObj;
    }

    @Bean
    public ContentVerifierProvider bcECContentVerifierProviderBuilder() throws Exception {
        return new BcECContentVerifierProviderBuilder(new DefaultDigestAlgorithmIdentifierFinder())
                .build(caCertHolder());
    }


    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("identity-service-api-");
        executor.setTaskDecorator(runnable -> () -> {
            try {
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null) {
                    MDC.put("userId", authentication.getName());
                }
                runnable.run();
            } finally {
                MDC.remove("userId");
            }
        });
        executor.initialize();
        return executor;
    }


    @Bean
    @SuppressWarnings("unchecked")
    public RouteBuilder cscaRequestRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from(Constants.UPDATE_CSCA_MASTER_LIST_ROUTE)
                        .threads().executorService(threadPoolTaskExecutor().getThreadPoolExecutor())
                        .log("Received ${body}")
                        .process(exchange -> {
                            CscaMasterListUploadRequest request = exchange.getIn().getBody(CscaMasterListUploadRequest.class);
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
                        .process(exchange -> {
                            boolean isValid = false;
                            CMSSignedData signature = new CMSSignedData((byte[]) exchange.getIn().getBody());
                            Store<?> cs = signature.getCertificates();
                            SignerInformationStore signers = signature.getSignerInfos();
                            Collection<?> c = signers.getSigners();
                            for (Object o : c) {
                                try {
                                    SignerInformation signer = (SignerInformation) o;
                                    Collection<?> certCollection = cs.getMatches(signer.getSID());
                                    Iterator<?> certIt = certCollection.iterator();
                                    X509CertificateHolder signedCert = (X509CertificateHolder) certIt.next();
                                    isValid = signedCert
                                            .isSignatureValid(bcECContentVerifierProviderBuilder());
                                    log.info("Is valid signed cert? {}", isValid);
                                } catch (Exception e) {
                                    log.error("Failed to verify cert", e);
                                    isValid = false;
                                }
                            }
                            if (!isValid) {
                                exchange.setException(new IllegalStateException(
                                        "Signing cert could not be validated"
                                ));
                            }
                        })
                        .log("Performed decryption, preparing to upload to S3")
                        .process(exchange -> {
                            File scratchFile = File.createTempFile(UUID.randomUUID().toString(), ".ml");
                            FileUtils.copyInputStreamToFile(exchange.getIn().getBody(InputStream.class), scratchFile);
                            PutObjectResult result = amazonS3.putObject(
                                    awsProperties.getCscaMasterListBucketName(), CSCA_MASTER_LIST_KEY,
                                    scratchFile);
                            exchange.getIn().setBody(result.getETag());
                        }).log("Uploaded to S3 with eTag: ${body}").end();

            }
        };
    }

}
