package io.digital.patterns.identity.api.security

import org.springframework.security.oauth2.jwt.Jwt
import spock.lang.Specification

import static org.springframework.security.oauth2.jwt.JwtClaimNames.SUB

class AudienceValidatorSpec extends Specification {

    def underTest = new AudienceValidator(['cop-ui'])

    def 'can validate audience'() {
        given: 'a jwt token with a valid audience'
        Jwt jwt = Jwt.withTokenValue('token')
                .header("alg", "none")
                .audience(['cop-ui'])
                .claim(SUB, "user")
                .claim("email", "email")
                .claim("roles", ['test'])
                .claim("scope", "read").build()

        when: 'token is validated'
        def result = underTest.validate(jwt)

        then: 'result should be success'
        !result.hasErrors()
    }

    def 'returns error if invalid audience'() {
        given: 'a jwt token with a invalid audience'
        Jwt jwt = Jwt.withTokenValue('token')
                .header("alg", "none")
                .audience(['fake-ui'])
                .claim(SUB, "user")
                .claim("email", "email")
                .claim("roles", ['test'])
                .claim("scope", "read").build()

        when: 'token is validated'
        def result = underTest.validate(jwt)

        then: 'result should have error'
        result.hasErrors()
        result.errors.first().errorCode == 'invalid_token'
        result.errors.first().description == 'The required audience is missing'
    }
}
