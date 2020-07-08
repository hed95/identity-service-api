package io.digital.patterns.identity.api.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AuthorizationChecker {

    public final List<String> readRoles;
    public final List<String> updateRoles;

    public AuthorizationChecker( @Value("${api.read.roles:}")List<String> readRoles,
                                 @Value("${api.update.roles:}")List<String> updateRoles) {
        this.readRoles = readRoles;
        this.updateRoles = updateRoles;
    }

    public boolean hasReadRoles(Authentication authentication) {
        if (readRoles.isEmpty()) {
            return false;
        }
        return getRoles(authentication).stream().anyMatch(readRoles::contains);
    }

    public boolean hasUpdateRoles(Authentication authentication) {
        if (updateRoles.isEmpty()) {
            return false;
        }
        return getRoles(authentication).stream().anyMatch(updateRoles::contains);
    }

    private List<String> getRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
    }
}
