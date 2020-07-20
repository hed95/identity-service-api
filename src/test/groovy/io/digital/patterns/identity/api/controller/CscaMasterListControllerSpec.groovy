package io.digital.patterns.identity.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.digital.patterns.identity.api.model.CscaMasterList
import io.digital.patterns.identity.api.model.CscaMasterListUploadRequest
import io.digital.patterns.identity.api.security.AuthorizationChecker
import io.digital.patterns.identity.api.service.CscaMasterListService
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [CscaMasterListController])
class CscaMasterListControllerSpec extends Specification {

    @Autowired
    private WebApplicationContext context

    private MockMvc mvc

    @SpringBean
    private CscaMasterListService cscaMasterListService = Mock()

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


    def 'throws 400 is data is missing'() {
        expect: '400 response'
        def list = new CscaMasterListUploadRequest()
        mvc.perform(put('/csca-masterlist')
                .content(new ObjectMapper().writeValueAsString(list))
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt()
                        .authorities([new SimpleGrantedAuthority('read'),
                                      new SimpleGrantedAuthority('update')])))
                .andExpect(status().isBadRequest())
    }

    def 'can start a master list put'() {
        given: 'upload service is called'
        1 * cscaMasterListService.upload(_)

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
        given: 'service is not called'
        0 * cscaMasterListService.upload(_)

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

    def 'can get list'() {
        given: 'upload service is called'
        def list = new CscaMasterList()
        list.etag = 'etag'
        list.content = 'content'
        list.lastModified = new Date()
        1 * cscaMasterListService.get(null) >> list

        expect: '200 response'
        mvc.perform(get('/csca-masterlist')
                .with(jwt()
                        .authorities([new SimpleGrantedAuthority('read')])))
                .andExpect(status().isOk())
    }

    def '403 if get role not present'() {
        expect: '403 response'
        mvc.perform(get('/csca-masterlist')
                .with(jwt()
                        .authorities([new SimpleGrantedAuthority('fake')])))
                .andExpect(status().isForbidden())
    }

    def '304 if etag same'() {
        given: 'upload service is called'
        def list = new CscaMasterList()
        list.etag = 'etag'
        list.content = null
        list.lastModified = new Date()
        1 * cscaMasterListService.get('etag') >> list

        expect: '304 response'
        mvc.perform(get('/csca-masterlist').header("ETag", 'etag')
                .with(jwt()
                        .authorities([new SimpleGrantedAuthority('read')])))
                .andExpect(status().isNotModified())
    }

}
