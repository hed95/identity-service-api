package io.digital.patterns.identity.api;

import io.prometheus.client.exporter.common.TextFormat;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Configuration
@SecurityScheme(
        name = "oauth2",
        type = SecuritySchemeType.OAUTH2,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        flows = @OAuthFlows(
                implicit = @OAuthFlow(
                        authorizationUrl = "${spring.security.oauth2.resourceserver.jwt.issuer-uri}/protocol/openid-connect/auth",
                        tokenUrl = "${spring.security.oauth2.resourceserver.jwt.issuer-uri}/protocol/openid-connect/token",
                        scopes = {@OAuthScope(name = "openid"), @OAuthScope(name = "email")}
                )
        ))
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        SecurityRequirement securityItem = new SecurityRequirement();
        securityItem.addList("oauth2");
        return new OpenAPI()
                .info(new Info().title("Identity Service API")
                        .version("1.0"))
                .addSecurityItem(securityItem);
    }

    @Configuration
    public static class ActuatorEndpointConfig {

        private static final List<String> MEDIA_TYPES = Arrays
                .asList("application/json", ActuatorMediaType.V2_JSON,
                        ActuatorMediaType.V3_JSON,
                        "application/hal+json");

        @Bean
        public EndpointMediaTypes endpointMediaTypes() {
            return new EndpointMediaTypes(MEDIA_TYPES, MEDIA_TYPES);
        }
    }
}
