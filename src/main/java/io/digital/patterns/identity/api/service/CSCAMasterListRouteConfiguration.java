package io.digital.patterns.identity.api.service;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CSCAMasterListRouteConfiguration {

    @Bean
    public RouteBuilder cscaMasterListUpdateRoute() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {

            }
        };
    }
}
