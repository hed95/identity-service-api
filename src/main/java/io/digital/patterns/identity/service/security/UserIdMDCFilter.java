package io.digital.patterns.identity.service.security;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

@Component
@Slf4j
public class UserIdMDCFilter extends OncePerRequestFilter {

    private final ApplicationEventPublisher applicationEventPublisher;

    AntPathRequestMatcher matcher = new AntPathRequestMatcher("/actuator/health", "GET");

    public UserIdMDCFilter(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {


        if (!matcher.matches(request)) {
            try {
                final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null) {
                    AuditEvent event = new AuditEvent(
                            request.getRequestURI(),
                            authentication.getName(),
                            new Date(),
                            request.getRemoteAddr(),
                            request.getMethod(),
                            request.getAuthType()
                    );
                    applicationEventPublisher.publishEvent(event);

                    MDC.put("userId", authentication.getName());
                    log.debug("Executing {}", request.getRequestURI());
                }
                filterChain.doFilter(request, response);
            } finally {
                log.debug("Executed {}", request.getRequestURI());
                MDC.remove("userId");
            }
        } else {
            filterChain.doFilter(request, response);
        }

    }
}
