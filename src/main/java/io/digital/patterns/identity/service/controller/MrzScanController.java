package io.digital.patterns.identity.service.controller;

import io.digital.patterns.identity.service.MrzService;
import io.digital.patterns.identity.service.model.MrzScan;
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
public class MrzScanController {

    private final MrzService mrzService;

    public MrzScanController(MrzService mrzService) {
        this.mrzService = mrzService;
    }

    @GetMapping(path="/{correlationId}")
    public List<MrzScan> getById(@PathVariable String correlationId) {
        return mrzService.getScans(correlationId);
    }

    @PostMapping(path = "/{correlationId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(code = HttpStatus.CREATED)
    @PreAuthorize(value = "@authorizationChecker.hasUpdateRoles(authentication)")
    public void create(@RequestBody @Valid MrzScan mrzScan) {
        mrzService.create(mrzScan);
    }

}
