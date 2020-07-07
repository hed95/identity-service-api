package io.digital.patterns.identity.service.model;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
public class MrzScan {

    @NotNull
    private String correlationId;

    @NotNull
    private Date dateOfScan;

    @NotNull
    private String scanningOfficer;

    @NotNull
    private String result;

    @NotNull
    private String issuingCountry;

    @NotNull
    private byte[] faceImage;

    @NotNull
    private String dob;

    @NotNull
    private String doe;

    @NotNull
    private String documentNumber;

    @NotNull
    private String primaryIdentifier;

    @NotNull
    private String mrzString;

    private String secondaryIdentifier;
}
