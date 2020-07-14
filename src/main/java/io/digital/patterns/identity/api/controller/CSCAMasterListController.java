package io.digital.patterns.identity.api.controller;


import io.digital.patterns.identity.api.model.CSCAMasterListRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/csca-masterlist")
@PreAuthorize(value = "@authorizationChecker.hasReadRoles(authentication)")
@Tag(name="CSCA master list", description = "Storing and retrieving csca master list")
public class CSCAMasterListController {

    private final ProducerTemplate producerTemplate;

    public CSCAMasterListController(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }

    @PutMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize(value = "@authorizationChecker.hasUpdateRoles(authentication)")
    public void create(@RequestBody CSCAMasterListRequest request) {
        producerTemplate.asyncSendBody(
                "direct:update-masterlist-route", request
        );
    }
}
