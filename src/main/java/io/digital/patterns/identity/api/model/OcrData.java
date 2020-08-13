package io.digital.patterns.identity.api.model;

import lombok.Data;

@Data
public class OcrData {
    private Person person;
    private Document document;
    private Image image;

    @Data
    public static class Image {
        private String mrz;
        private String photo;
        private String signature;
    }
}
