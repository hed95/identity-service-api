package io.digital.patterns.identity.api.audit;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuditEventListener {

    @EventListener
    public void handle(AuditEvent auditEvent) {
        log.info("Event {}", auditEvent);
    }
}
