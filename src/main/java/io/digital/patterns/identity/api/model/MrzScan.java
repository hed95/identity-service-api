package io.digital.patterns.identity.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;


@Data
public class MrzScan {

    @NotNull
    @Schema(
            description = "Identifier that can be used to link multiple scans. This is not a primary key",
            required = true,
            type = "String"
    )
    private String correlationId;
    @Schema(
            description = "Defaults to the current date if not provided"
    )
    private Date dateOfScan = new Date();
    @NotNull
    @Schema(
            description = "Officer who initiated the scan",
            required = true, example = "officer@example.com"
    )
    private String scanningOfficer;
    @NotNull
    @Schema(
            description = "Overall status of the scan. Whether it failed or succeeded",
            required = true,
            example = "FAILED",
            type = "String"
    )
    private String status;

    @Schema(description = "Supporting message if scan failed. " +
            "This should be human readable. Avoid putting stacktrace message. If possible use a code that can " +
            "be correlated against exceptions")
    private String message;

    @NotNull
    private Mrz mrz;

    @Schema(description = "OCR data, this is optional and not required")
    private OcrData ocrData;

    @Schema(description = "NFC data, this is optional and not required")
    private Nfc nfc;

}
