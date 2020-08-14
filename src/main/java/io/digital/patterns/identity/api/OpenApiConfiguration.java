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
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.PathParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomiser;
import org.springframework.boot.actuate.endpoint.http.ActuatorMediaType;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
                        .version("1.0")
                .contact(new Contact().name("Digital Patterns Limited")
                    .url("https://digitalpatterns.io").email("sales@digitalpatterns.io")))
                .addSecurityItem(securityItem);
    }

    @Bean
    public OpenApiCustomiser actuatorOpenApiCustomiser() {
        final Pattern pathPattern = Pattern.compile("\\{(.*?)}");
        return openApi -> openApi.getPaths().entrySet().stream()
                .filter(stringPathItemEntry -> stringPathItemEntry.getKey().startsWith("/actuator/"))
                .forEach(stringPathItemEntry -> {
                    if (stringPathItemEntry.getKey().equalsIgnoreCase("/actuator/prometheus")) {
                        stringPathItemEntry.getValue().getGet().getResponses()
                                .get("200")
                                    .setContent(new Content()
                                            .addMediaType("text/plain; version=0.0.4;charset=utf-8",
                                                    new MediaType().schema(new StringSchema())));
                    }
                    String path = stringPathItemEntry.getKey();
                    Matcher matcher = pathPattern.matcher(path);
                    while (matcher.find()) {
                        String pathParam = matcher.group(1);
                        PathItem pathItem = stringPathItemEntry.getValue();
                        pathItem.readOperations().forEach(operation ->
                                operation
                                        .addParametersItem(new PathParameter()
                                                .name(pathParam).schema(new StringSchema())));
                    }
                });
    }

    @Configuration
    public static class ActuatorEndpointConfig {

        private static final List<String> MEDIA_TYPES = Arrays
                .asList("application/json", ActuatorMediaType.V2_JSON,
                        ActuatorMediaType.V3_JSON,
                        "application/hal+json", TextFormat.CONTENT_TYPE_004);

        @Bean
        public EndpointMediaTypes endpointMediaTypes() {
            return new EndpointMediaTypes(MEDIA_TYPES, MEDIA_TYPES);
        }
    }
}
