package io.digital.patterns.identity.api.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.core.GrantedAuthorityDefaults;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final String ACTUATOR_HEALTH = "/actuator/health/**";
    private static final String ACTUATOR_METRICS = "/actuator/metrics/**";
    private static final String ACTUATOR_INFO = "/actuator/info/**";
    private static final String ACTUATOR_LOGGERS = "/actuator/loggers/**";
    private static final String ACTUATOR_PROMETHEUS = "/actuator/prometheus/**";
    private static final String ACTUATOR = "/actuator";

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuer;
    @Value("${api.allowedAudiences}")
    private List<String> allowedAudiences;
    @Value("${api.admin.roles:}")
    private List<String> adminRoles;


    private final KeycloakJwtConverter keycloakJwtConverter;

    public SecurityConfiguration(KeycloakJwtConverter keycloakJwtConverter) {
        this.keycloakJwtConverter = keycloakJwtConverter;
        SecurityContextHolder
                .setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Override
    public void configure(final HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.GET, ACTUATOR_HEALTH).permitAll()
                .antMatchers(HttpMethod.GET,ACTUATOR_METRICS).permitAll()
                .antMatchers(HttpMethod.GET,ACTUATOR_INFO).permitAll()
                .antMatchers(HttpMethod.GET, ACTUATOR_LOGGERS).permitAll()
                .antMatchers(ACTUATOR, "GET").permitAll()
                .antMatchers(HttpMethod.GET, ACTUATOR_PROMETHEUS).permitAll()
                .antMatchers("/swagger/**").permitAll()
                .antMatchers("/docs/**").permitAll()
                .antMatchers(HttpMethod.POST,ACTUATOR_LOGGERS)
                    .hasAnyAuthority(adminRoles.toArray(new String[]{}))
                .anyRequest()
                .authenticated()
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and().oauth2ResourceServer()
                .jwt()
                .jwtAuthenticationConverter(keycloakJwtConverter);
    }

    @Bean
    public NimbusJwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder)
                JwtDecoders.fromOidcIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> audienceValidator = new AudienceValidator(allowedAudiences);
        OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> withAudience = new DelegatingOAuth2TokenValidator<>(withIssuer, audienceValidator);

        jwtDecoder.setJwtValidator(withAudience);

        return jwtDecoder;
    }

    @Bean
    GrantedAuthorityDefaults grantedAuthorityDefaults() {
        return new GrantedAuthorityDefaults("");
    }

}
