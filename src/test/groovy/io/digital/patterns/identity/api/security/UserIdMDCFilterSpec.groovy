package io.digital.patterns.identity.api.security

import org.spockframework.spring.SpringBean
import org.springframework.context.ApplicationEventPublisher
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import spock.lang.Specification

class UserIdMDCFilterSpec extends Specification {

    @SpringBean
    def applicationEventPublisher = Mock(ApplicationEventPublisher)

    UserIdMDCFilter underTest

    def setup() {
        underTest = new UserIdMDCFilter(applicationEventPublisher)
        SecurityContextHolder.clearContext()
    }

    def 'can publish an event'() {
        given: 'a request and respons'
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

    def 'event not published if health check request'() {
        given: 'a request and respons'
        def mockRequest = new MockHttpServletRequest()
        mockRequest.setPathInfo('/actuator/health')
        mockRequest.setMethod('GET')
        def mockResponse = new MockHttpServletResponse()
        def mockChain = new MockFilterChain()

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new TestingAuthenticationToken('test@test.com', new Object()))
        SecurityContextHolder.setContext(context)

        when: 'filter invoked'
        underTest.doFilterInternal(mockRequest, mockResponse, mockChain)

        then: 'publish event not triggered'
        0 * applicationEventPublisher.publishEvent(_)

    }
}
