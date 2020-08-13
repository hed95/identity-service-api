package io.digital.patterns.identity.api.model;

import lombok.Data;

@Data
public class Person {

    private String surname;
    private String[] givenNames;
    private String nationality;
    private String dateOfBirth;
    private String placeOfBirth;
    private String sex;
}
