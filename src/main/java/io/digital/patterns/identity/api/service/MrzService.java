package io.digital.patterns.identity.api.service;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.digital.patterns.identity.api.aws.AwsProperties;
import io.digital.patterns.identity.api.model.MrzScan;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static java.io.File.createTempFile;
import static java.util.stream.Collectors.toList;

@Slf4j
@Service
public class MrzService {

    private final AmazonS3 amazonS3;
    private final AwsProperties awsProperties;
    private final ObjectMapper objectMapper;
    private final static
            SimpleDateFormat FORMAT = new SimpleDateFormat("YYYYMMDD'T'HHmmss");

    public MrzService(AmazonS3 amazonS3,
                      AwsProperties awsProperties,
                      ObjectMapper objectMapper) {
        this.amazonS3 = amazonS3;
        this.awsProperties = awsProperties;
        this.objectMapper = objectMapper;
    }

    public List<MrzScan> getScans(String correlationId) {

        ObjectListing objectListing = amazonS3.listObjects(awsProperties.getBucketName(), correlationId);
        List<MrzScan> scans = new ArrayList<>();

        if (objectListing.getObjectSummaries().isEmpty()) {
            log.info("No scans found for '{}'", correlationId);
            return scans;
        }
        return objectListing.getObjectSummaries().stream().map((summary) -> {
            S3Object object = amazonS3.getObject(awsProperties.getBucketName(), summary.getKey());
            try {
                String json = IOUtils.toString(object.getObjectContent(),
                        StandardCharsets.UTF_8);
                return objectMapper.readValue(json, MrzScan.class);
            } catch (Exception e) {
                log.error("Unable to get data stream for '{}'", summary.getKey(), e);
                return null;
            }
        }).filter(Objects::nonNull).collect(toList());
    }

    public String create(@Valid MrzScan mrzScan) {

        try {
            String key = key(mrzScan.getCorrelationId(), mrzScan.getScanningOfficer(),
                    mrzScan.getDateOfScan());

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.addUserMetadata("submittedby", mrzScan.getScanningOfficer());
            metadata.addUserMetadata("submissiondate", mrzScan.getDateOfScan().toString());
            metadata.addUserMetadata("correlationId", mrzScan.getCorrelationId());
            metadata.addUserMetadata("primaryidentifier", mrzScan.getPrimaryIdentifier());
            File scratchFile = createTempFile(UUID.randomUUID().toString(), ".json");

            FileUtils.copyInputStreamToFile(IOUtils.toInputStream(
                    objectMapper.writeValueAsString(mrzScan), "UTF-8"), scratchFile);

            PutObjectRequest request = new PutObjectRequest(awsProperties.getBucketName(), key, scratchFile);
            request.setMetadata(metadata);
            final PutObjectResult putObjectResult = amazonS3.putObject(request);
            log.info("Uploaded to S3 '{}'", putObjectResult.getETag());
            return mrzScan.getCorrelationId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String key(String businessKey, String email, Date submissionDate) {
        StringBuilder keyBuilder = new StringBuilder();
        String timeStamp = FORMAT.format(submissionDate);

        return keyBuilder.append(businessKey)
                .append("/").append(email).append("-").append(timeStamp).append(".json")
                .toString();

    }
}
