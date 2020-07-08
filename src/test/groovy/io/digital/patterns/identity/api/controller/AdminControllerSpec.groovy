package io.digital.patterns.identity.api.controller

import io.digital.patterns.identity.api.security.AuthorizationChecker
import io.digital.patterns.identity.api.service.MrzService
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [AdminController])
class AdminControllerSpec extends Specification {

    @Autowired
    private WebApplicationContext context

    private MockMvc mvc

    @SpringBean
    private MrzService mrzService = Mock()

    @SpringBean
    private AuthorizationChecker authorizationChecker = new AuthorizationChecker(['read'], ['update'], ['admin'])

    @SpringBean
    private JwtDecoder jwtDecoder = Mock()

    def setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build()
    }

    def 'cannot delete if admin role not present'() {
        expect: '403 response'
        mvc.perform(delete('/admin/mrz/id')
                .with(jwt()
                        .authorities([
                                new SimpleGrantedAuthority('read'),
                                      new SimpleGrantedAuthority('fake')])))
                .andExpect(status().isForbidden())
    }

    def 'can delete if admin role present'() {

        given: 'delete is called'
        1 * mrzService.delete('id')

        expect: '403 response'
        mvc.perform(delete('/admin/mrz/id')
                .with(jwt()
                        .authorities([
                                new SimpleGrantedAuthority('read'),
                                new SimpleGrantedAuthority('update'),
                                new SimpleGrantedAuthority('admin')])))
                .andExpect(status().isOk())



    }
}
