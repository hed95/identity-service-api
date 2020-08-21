package io.digital.patterns.identity.api.service;


import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.digital.patterns.identity.api.ProcessInstance;
import io.digital.patterns.identity.api.aws.AwsProperties;
import io.digital.patterns.identity.api.model.MrzScan;
import io.digital.patterns.identity.api.model.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate;
    private final String workflowUrl;

    public MrzService(AmazonS3 amazonS3,
                      AwsProperties awsProperties,
                      ObjectMapper objectMapper, RestTemplate restTemplate,
                      @Value("${workflowApi.url}") String workflowUrl) {
        this.amazonS3 = amazonS3;
        this.awsProperties = awsProperties;
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
        this.workflowUrl = workflowUrl;
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
            File scratchFile = createTempFile(UUID.randomUUID().toString(), ".json");

            FileUtils.copyInputStreamToFile(IOUtils.toInputStream(
                    objectMapper.writeValueAsString(mrzScan), "UTF-8"), scratchFile);

            PutObjectRequest request = new PutObjectRequest(awsProperties.getBucketName(), key, scratchFile);
            request.setMetadata(metadata);
            final PutObjectResult putObjectResult = amazonS3.putObject(request);
            log.info("Uploaded to S3 '{}'", putObjectResult.getETag());

            Workflow workflow = mrzScan.getWorkflow();
            if (workflow != null) {
                JwtAuthenticationToken authenticationToken = (JwtAuthenticationToken)
                        SecurityContextHolder.getContext().getAuthentication();
                log.info("This submission will be sent to workflow service {}, {}, {}",
                        workflow.getWorkflowUrl(),
                        workflow.getProcessKey(),
                        workflow.getVariableName());

                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                httpHeaders.setAccept(List.of(MediaType.APPLICATION_JSON));
                httpHeaders.setBearerAuth(authenticationToken.getToken().getTokenValue());
                JSONObject payload = new JSONObject();
                payload.put("businessKey", mrzScan.getCorrelationId());

                JSONObject variable = new JSONObject();
                JSONObject alertJson = new JSONObject();
                alertJson.put("value", objectMapper.writeValueAsString(mrzScan));
                alertJson.put("type", "json");
                variable.put(workflow.getVariableName(), alertJson);
                payload.put("variables", variable);

                HttpEntity<?> httpEntity = new HttpEntity<>(
                        payload.toString(),
                        httpHeaders
                );

                String url = Optional.ofNullable(workflow.getWorkflowUrl())
                            .orElse(this.workflowUrl) + "/camunda/engine-rest/process-definition/key/"
                        + workflow.getProcessKey() + "/start";
                ProcessInstance processInstance = restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        httpEntity,
                        ProcessInstance.class,
                        new HashMap<>()
                ).getBody();
                log.info("Process instance started {} for mrz scan {}",
                        Objects.requireNonNull(processInstance).getId(),
                        mrzScan.getCorrelationId());

            }

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

    public void delete(String correlationId) {
        log.info("Deleting scans with {} as id", correlationId);
        ObjectListing objectListing = amazonS3
                .listObjects(awsProperties.getBucketName(), correlationId);

        objectListing.getObjectSummaries().forEach((summary) ->
                amazonS3.deleteObject(awsProperties.getBucketName(), summary.getKey()));
        log.info("Deleted all contents under {}",correlationId);
    }
}
