package io.digital.patterns.identity.api.model;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class CscaMasterListUploadRequest {

    @NotNull
    private String bucketName;
    @NotNull
    private String fileName;
}
