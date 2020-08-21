package io.digital.patterns.identity.api;

import lombok.Data;

@Data
public class ProcessInstance {
    private String id;
    private String businessKey;
    private String processDefinitionId;
}
