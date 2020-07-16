package io.digital.patterns.identity.api.controller;

import io.digital.patterns.identity.api.model.MrzScan;
import io.digital.patterns.identity.api.service.MrzService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Slf4j
@RestController
@RequestMapping(path = "/mrz")
@PreAuthorize(value = "@authorizationChecker.hasReadRoles(authentication)")
@Tag(name="MRZ scans", description = "Storing and retrieving mrz scans from repository")
public class MrzScanController {

    private final MrzService mrzService;

    public MrzScanController(MrzService mrzService) {
        this.mrzService = mrzService;
    }

    @GetMapping(path="/{correlationId}")
    @Operation(summary = "Retrieves a list of mrz scans for a given correlation id")
    public List<MrzScan> getById(@PathVariable @Parameter(description="ID that can link a list of scans",
            required = true) String correlationId) {
        return mrzService.getScans(correlationId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = HttpStatus.CREATED)
    @PreAuthorize(value = "@authorizationChecker.hasUpdateRoles(authentication)")
    @Operation(summary = "Creates an MRZ scan")
    public void create(@RequestBody @Valid @Parameter(description="MRZ scan object", required = true) MrzScan mrzScan) {
        mrzService.create(mrzScan);
    }

}
