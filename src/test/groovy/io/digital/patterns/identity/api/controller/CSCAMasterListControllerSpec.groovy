package io.digital.patterns.identity.api.controller


import io.digital.patterns.identity.api.security.AuthorizationChecker
import org.apache.camel.ProducerTemplate
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import spock.lang.Specification

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static io.digital.patterns.identity.api.service.Routes.UPDATE_CSCA_MASTER_LIST_ROUTE

@WebMvcTest(controllers = [CSCAMasterListController])
class CSCAMasterListControllerSpec extends Specification {

    @Autowired
    private WebApplicationContext context

    private MockMvc mvc

    @SpringBean
    private ProducerTemplate producerTemplate = Mock()

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

    def 'can start a master list put'() {
        given: 'producer template is called'
        1 * producerTemplate.asyncSendBody(UPDATE_CSCA_MASTER_LIST_ROUTE, _)

        expect: '202 response'
        mvc.perform(put('/csca-masterlist')
                .content('''{
                                     "bucketName" : "test",
                                     "fileName": "fileName"
                                    }''')
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt()
                        .authorities([new SimpleGrantedAuthority('read'),
                                      new SimpleGrantedAuthority('update')])))
                .andExpect(status().isAccepted())
    }

    def '403 on put if not authorized'() {
        given: 'producer template is called'
        0 * producerTemplate.asyncSendBody("direct:update-csca-master-list-route", _)

        expect: '403 response'
        mvc.perform(put('/csca-masterlist')
                .content('''{
                                     "bucketName" : "test",
                                     "fileName": "fileName"
                                    }''')
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt()
                        .authorities([new SimpleGrantedAuthority('read'),
                                      new SimpleGrantedAuthority('fake')])))
                .andExpect(status().isForbidden())
    }

}
