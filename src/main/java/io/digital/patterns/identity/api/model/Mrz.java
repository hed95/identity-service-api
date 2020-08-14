package io.digital.patterns.identity.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class Mrz {

    @Schema(
            description = "Type of scan",
            example = "TD1, TD2 or TD3",
            enumAsRef = true
    )
    @NotNull
    private MrzType type;
    private String issuingCountry;
    private String surname;
    private String[] givenNames;
    private String documentNumber;
    private String nationality;
    private String dateOfBirth;
    private String sex;
    private String dateOfExpiry;
    private String personalNumber;
    private String documentType;
    @Schema(description = "The raw mrz as base 64 encoded string")
    private String raw;
}
