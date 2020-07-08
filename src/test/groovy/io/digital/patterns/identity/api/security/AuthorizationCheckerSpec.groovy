package io.digital.patterns.identity.api.security

import org.springframework.security.oauth2.jwt.Jwt
import spock.lang.Specification

import static org.springframework.security.oauth2.jwt.JwtClaimNames.SUB

class AuthorizationCheckerSpec extends Specification {

    def underTest = new AuthorizationChecker(['read'], ['update'])
    def converter = new KeycloakJwtConverter()

    def 'returns true if authentication has read role'() {

        given: 'a jwt token'
        Jwt jwt = Jwt.withTokenValue('token')
                .header("alg", "none")
                .claim(SUB, "user")
                .claim("email", "email")
                .claim("realm_access", [
                        'roles' : ['read']
                ])
                .claim("scope", "read").build()

        def authentication = converter.convert(jwt)

        when: 'authorization checked'
        def result = underTest.hasReadRoles(authentication)

        then: 'result should be true'
        result
    }

    def 'returns false if no read role configured'() {

        given: 'a jwt token'
        def underTest = new AuthorizationChecker([], ['update'])
        Jwt jwt = Jwt.withTokenValue('token')
                .header("alg", "none")
                .claim(SUB, "user")
                .claim("email", "email")
                .claim("realm_access", [
                        'roles' : ['read']
                ])
                .claim("scope", "read").build()

        def authentication = converter.convert(jwt)

        when: 'authorization checked'
        def result = underTest.hasReadRoles(authentication)

        then: 'result should be false'
        !result
    }

    def 'returns false if no update role configured'() {

        given: 'a jwt token'
        def underTest = new AuthorizationChecker([], [])
        Jwt jwt = Jwt.withTokenValue('token')
                .header("alg", "none")
                .claim(SUB, "user")
                .claim("email", "email")
                .claim("realm_access", [
                        'roles' : ['read']
                ])
                .claim("scope", "read").build()

        def authentication = converter.convert(jwt)

        when: 'authorization checked'
        def result = underTest.hasUpdateRoles(authentication)

        then: 'result should be false'
        !result
    }

    def 'returns false if authentication does not have read role'() {
        given: 'a jwt token'
        Jwt jwt = Jwt.withTokenValue('token')
                .header("alg", "none")
                .claim(SUB, "user")
                .claim("email", "email")
                .claim("realm_access", [
                        'roles' : ['randowm']
                ])
                .claim("scope", "read").build()

        def authentication = converter.convert(jwt)

        when: 'authorization checked'
        def result = underTest.hasReadRoles(authentication)

        then: 'result should be false'
        !result
    }

    def 'returns false if authentication does not have update role'() {
        given: 'a jwt token'
        Jwt jwt = Jwt.withTokenValue('token')
                .header("alg", "none")
                .claim(SUB, "user")
                .claim("email", "email")
                .claim("realm_access", [
                        'roles' : ['randowm']
                ])
                .claim("scope", "read").build()

        def authentication = converter.convert(jwt)

        when: 'authorization checked'
        def result = underTest.hasUpdateRoles(authentication)

        then: 'result should be false'
        !result
    }


    def 'returns true if authentication has update role'() {

        given: 'a jwt token'
        Jwt jwt = Jwt.withTokenValue('token')
                .header("alg", "none")
                .claim(SUB, "user")
                .claim("email", "email")
                .claim("realm_access", [
                        'roles' : ['update']
                ])
                .claim("scope", "read").build()

        def authentication = converter.convert(jwt)

        when: 'authorization checked'
        def result = underTest.hasUpdateRoles(authentication)

        then: 'result should be true'
        result
    }
}
