package io.digital.patterns.identity.api.audit

import io.digital.patterns.identity.api.audit.AuditEventFilter
import org.spockframework.spring.SpringBean
import org.springframework.context.ApplicationEventPublisher
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification
import spock.lang.Unroll

class AuditEventFilterSpec extends Specification {

    @SpringBean
    def applicationEventPublisher = Mock(ApplicationEventPublisher)

    AuditEventFilter underTest

    def setup() {
        underTest = new AuditEventFilter(applicationEventPublisher)
        SecurityContextHolder.clearContext()
    }

    def 'can publish an event'() {
        given: 'a request and response'
        def mockRequest = new MockHttpServletRequest()
        def mockResponse = new MockHttpServletResponse()
        def mockChain = new MockFilterChain()

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken('test@test.com', new Object()))
        SecurityContextHolder.setContext(context)

        when: 'filter invoked'
        underTest.doFilterInternal(mockRequest, mockResponse, mockChain)

        then: 'publish event triggered'
        1 * applicationEventPublisher.publishEvent(_)

    }

    @Unroll
    def 'event not published if actuator #endpoint request'() {

        0 * applicationEventPublisher.publishEvent(_)

        expect: 'no call to event publisher'
        def mockRequest = new MockHttpServletRequest()
        mockRequest.setPathInfo("/actuator/${endpoint}")
        mockRequest.setMethod('GET')
        def mockResponse = new MockHttpServletResponse()
        def mockChain = new MockFilterChain()

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken('test@test.com', new Object()))
        SecurityContextHolder.setContext(context)
        underTest.doFilterInternal(mockRequest, mockResponse, mockChain)


        where: 'endpoint is #endpoint'
        endpoint        | _
        'health'        | _
        'info'          | _
        'loggers'       | _
        'metrics'       | _
        'prometheus'    | _

    }
}
