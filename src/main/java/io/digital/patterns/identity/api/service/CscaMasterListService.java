package io.digital.patterns.identity.api.service;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import io.digital.patterns.identity.api.aws.AwsProperties;
import io.digital.patterns.identity.api.model.CscaMasterList;
import io.digital.patterns.identity.api.model.CscaMasterListUploadRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.nio.charset.StandardCharsets;

import static io.digital.patterns.identity.api.Constants.CSCA_MASTER_LIST_KEY;
import static io.digital.patterns.identity.api.Constants.UPDATE_CSCA_MASTER_LIST_ROUTE;

@Service
@Slf4j
public class CscaMasterListService {

    private final ProducerTemplate producerTemplate;
    private final AmazonS3 amazonS3;
    private final AwsProperties awsProperties;

    public CscaMasterListService(ProducerTemplate producerTemplate, AmazonS3 amazonS3, AwsProperties awsProperties) {
        this.producerTemplate = producerTemplate;
        this.amazonS3 = amazonS3;
        this.awsProperties = awsProperties;
    }

    public void upload(@Valid CscaMasterListUploadRequest request) {
        log.info("Initiating new upload");
        producerTemplate.asyncSendBody(
                UPDATE_CSCA_MASTER_LIST_ROUTE, request
        );
        log.info("Upload triggered");
    }

    public CscaMasterList get(String submittedETag) {
        try {
            CscaMasterList list = new CscaMasterList();
            String cscaMasterListBucketName = awsProperties.getCscaMasterListBucketName();
            if (submittedETag == null) {
                log.info("No etag present...so loading list");
                S3Object object = amazonS3.getObject(cscaMasterListBucketName, CSCA_MASTER_LIST_KEY);
                String content = IOUtils.toString(object.getObjectContent(), StandardCharsets.UTF_8);
                String eTag = object.getObjectMetadata().getETag();
                list.setContent(content);
                list.setEtag(eTag);
            } else {
                log.info("etag present...checking if list needs to be loaded");
                ObjectMetadata objectMetadata = amazonS3.getObjectMetadata(cscaMasterListBucketName,
                        CSCA_MASTER_LIST_KEY);
                String eTag = objectMetadata.getETag();
                if (!eTag.equalsIgnoreCase(submittedETag)) {
                    log.info("etag was present but it was not the same as S3 so loading list");
                    S3Object object = amazonS3.getObject(cscaMasterListBucketName, CSCA_MASTER_LIST_KEY);
                    String content = IOUtils.toString(object.getObjectContent(), StandardCharsets.UTF_8);
                    list.setContent(content);
                    list.setEtag(eTag);
                } else {
                    log.info("eTag the same so not returning list");
                }
            }
            return list;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
