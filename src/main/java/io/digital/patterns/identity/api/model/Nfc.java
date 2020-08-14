package io.digital.patterns.identity.api.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

@Data
public class Nfc {

    private SigningCertificate documentSigning;
    private SigningCertificate countrySigning;
    private String chipAuthenticationStatus;
    private String activeAuthenticationStatus;
    private Person person;
    private NfcDocument document;


    @Data
    @EqualsAndHashCode(callSuper=true)
    public static class NfcDocument extends Document {
        @Schema(description = "A list of base 64 encoded strings of images")
        private String[] photos;
        private String chipMrz;
    }

    @Data
    public static class SigningCertificate {
        @Schema(description = "Base 64 encoded pem string",
        required = true)
        @NotNull
        private String certificate;
        private String status;
    }
}
