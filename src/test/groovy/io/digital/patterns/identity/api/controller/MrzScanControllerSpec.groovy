package io.digital.patterns.identity.api.controller

import com.fasterxml.jackson.databind.ObjectMapper
import io.digital.patterns.identity.api.model.Mrz
import io.digital.patterns.identity.api.model.MrzScan
import io.digital.patterns.identity.api.model.MrzType
import io.digital.patterns.identity.api.security.AuthorizationChecker
import io.digital.patterns.identity.api.service.MrzService
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [MrzScanController])
class MrzScanControllerSpec extends Specification {

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

    def 'can get mrz scans'() {
        given: 'repository returns configurations'
        mrzService.getScans(_) >> [new MrzScan()]

        expect: '200 response'
        mvc.perform(get('/mrz/id')
                .with(jwt()
                        .authorities([new SimpleGrantedAuthority('read')])))
                .andExpect(status().isOk())
    }

    def 'cannot get mrz scans if no read role'() {
        expect: '403 response'
        mvc.perform(get('/mrz/id')
                .with(jwt()
                        .authorities([new SimpleGrantedAuthority('fake')])))
                .andExpect(status().isForbidden())
    }

    def 'can post mrz'() {
        given: 'an MRZ scan'
        def scan = new MrzScan()
        scan.correlationId = 'id'
        scan.dateOfScan = new Date()
        scan.scanningOfficer = 'test@test.com'
        scan.status = 'SUCCESS'

        scan.mrz = new Mrz()
        scan.mrz.dateOfExpiry = '27/12/2000'
        scan.mrz.dateOfBirth = '24/12/2000'
        scan.mrz.type = MrzType.TD1

        expect: '201 response'
        mvc.perform(post('/mrz')
        .content(new ObjectMapper().writeValueAsString(scan))
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt()
                        .authorities([new SimpleGrantedAuthority('read'),
                                      new SimpleGrantedAuthority('update')])))
                .andExpect(status().isCreated())
    }

    def 'cannot post mrz if correlation id is missing'() {
        given: 'an MRZ scan'
        def scan = new MrzScan()
        scan.dateOfScan = new Date()
        scan.scanningOfficer = 'test@test.com'
        scan.status = 'SUCCESS'

        scan.mrz = new Mrz()
        scan.mrz.dateOfExpiry = '27/12/2000'
        scan.mrz.dateOfBirth = '24/12/2000'
        scan.mrz.type = MrzType.TD1

        expect: '201 response'
        mvc.perform(post('/mrz')
                .content(new ObjectMapper().writeValueAsString(scan))
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt()
                        .authorities([new SimpleGrantedAuthority('read'),
                                      new SimpleGrantedAuthority('update')])))
                .andExpect(status().isBadRequest())
    }

    def 'cannot post mrz if user does not have role'() {
        given: 'an MRZ scan'
        def scan = new MrzScan()
        scan.correlationId = 'id'
        scan.dateOfScan = new Date()
        scan.scanningOfficer = 'test@test.com'
        scan.status = 'SUCCESS'

        scan.mrz = new Mrz()
        scan.mrz.dateOfExpiry = '27/12/2000'
        scan.mrz.dateOfBirth = '24/12/2000'
        scan.mrz.type = MrzType.TD1


        expect: '403 response'
        mvc.perform(post('/mrz')
                .content(new ObjectMapper().writeValueAsString(scan))
                .contentType(MediaType.APPLICATION_JSON)
                .with(jwt()
                        .authorities([new SimpleGrantedAuthority('read'),
                                      new SimpleGrantedAuthority('fake')])))
                .andExpect(status().isForbidden())
    }
}
