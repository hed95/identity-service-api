package io.digital.patterns.identity.service.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {


    private Collection<GrantedAuthority> getGrantedAuthorities(final Jwt jwt) {
        final Map<String, Object> realmAccess = (Map<String, Object>) jwt.getClaims().get("realm_access");
        return ((List<String>)realmAccess.get("roles"))
                .stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt source) {
        String name = source.getClaimAsString("email");
        return new JwtAuthenticationToken(source, getGrantedAuthorities(source), name);
    }
}

