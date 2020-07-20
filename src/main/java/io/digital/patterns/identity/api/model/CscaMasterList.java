package io.digital.patterns.identity.api.model;

import lombok.Data;

import java.util.Date;

@Data
public class CscaMasterList {
    private String etag;
    private String content;
    private Date lastModified;
}
