package io.digital.patterns.identity.service.security;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class AuditEvent {

    private String path;
    private String userId;
    private Date eventDate;
    private String ipAddress;
    private String method;
    private String authType;
}
