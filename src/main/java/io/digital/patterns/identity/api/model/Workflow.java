package io.digital.patterns.identity.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class Workflow {

    @Schema(description = "Name of the process that needs to be started", required = true)
    @NotNull
    private String processKey;

    @Schema(description = "Name of the variable this submission", required = true)
    @NotNull
    private String variableName;

    @Schema(description = "Optional, if this is not provided then the " +
            "workflow url configured for this service will be used.")
    private String workflowUrl;
}
