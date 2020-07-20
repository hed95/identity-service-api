package io.digital.patterns.identity.api.controller;


import io.digital.patterns.identity.api.model.CscaMasterList;
import io.digital.patterns.identity.api.model.CscaMasterListUploadRequest;
import io.digital.patterns.identity.api.service.CscaMasterListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping(path = "/csca-masterlist")
@PreAuthorize(value = "@authorizationChecker.hasReadRoles(authentication)")
@Tag(name = "CSCA master list", description = "Storing and retrieving csca master list")
@Slf4j
public class CscaMasterListController {

    private final CscaMasterListService cscaMasterListService;

    public CscaMasterListController(CscaMasterListService cscaMasterListService) {
        this.cscaMasterListService = cscaMasterListService;
    }


    @PutMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize(value = "@authorizationChecker.hasUpdateRoles(authentication)")
    @Operation(summary = "API for uploading a new CSCA UK Master list")
    public void create(@RequestBody @Valid @Parameter(description="Request object containing S3 bucket name and file",
            required = true) CscaMasterListUploadRequest request) {
        log.info("New master list upload request received");
        cscaMasterListService.upload(request);
    }


    @GetMapping(produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    @Operation(summary = "API getting the CSCA UK master list",
             description = "This API returns a ETag in the header. If you make a subsequent requests with the ETag " +
            "then the API will check if the content has changed. If the content has changed," +
            " you will receive the list with new ETag. If the content has not changed," +
            " you will get a 304 status code which means you should use the local copy",
    responses = {
            @ApiResponse(responseCode = "200", description = "Successful response with content"),
            @ApiResponse(responseCode = "403", description = "Caller does not have the appropriate " +
                    "roles to make this call",
                    content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "401", description = "Caller not authenticated to make this call",
                    content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "304", description = "Content has not changed so use local copy",
            content = @Content(schema = @Schema()))
    })
    public ResponseEntity<?> get(@RequestHeader(name = "ETag", required = false) String etag) {
        CscaMasterList cscaMasterList = cscaMasterListService.get(etag);
        if (cscaMasterList.getContent() == null) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .lastModified(cscaMasterList.getLastModified().getTime())
                    .eTag(etag)
                    .build();

        }
        return ResponseEntity.ok()
                .eTag(cscaMasterList.getEtag())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=csca-masterlist.ml")
                .lastModified(cscaMasterList.getLastModified().getTime())
                .body(cscaMasterList.getContent());
    }
}
