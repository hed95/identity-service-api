package io.digital.patterns.identity.api.model;

import lombok.Data;

@Data
public class CSCAMasterListRequest {

    private String bucketName;
    private String fileName;
}
