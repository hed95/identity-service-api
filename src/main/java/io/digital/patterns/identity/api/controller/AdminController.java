package io.digital.patterns.identity.api.controller;

import io.digital.patterns.identity.api.service.MrzService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/admin")
@PreAuthorize(value = "@authorizationChecker.hasReadRoles(authentication)")
public class AdminController {

    private final MrzService mrzService;

    public AdminController(MrzService mrzService) {
        this.mrzService = mrzService;
    }

    @DeleteMapping(path = "/mrz/{correlationId}")
    @PreAuthorize(value = "@authorizationChecker.hasUpdateRoles(authentication) && " +
            "@authorizationChecker.hasAdminRoles(authentication)")
    public void delete(@PathVariable String correlationId) {
        mrzService.delete(correlationId);
    }

}
