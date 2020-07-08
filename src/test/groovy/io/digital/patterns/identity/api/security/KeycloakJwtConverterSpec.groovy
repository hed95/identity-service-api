
package io.digital.patterns.identity.api.security

import org.springframework.security.oauth2.jwt.Jwt
import spock.lang.Specification

import static org.springframework.security.oauth2.jwt.JwtClaimNames.SUB

class KeycloakJwtConverterSpec extends Specification {
    KeycloakJwtConverter converter = new KeycloakJwtConverter()
    def 'can convert to authentication'() {
        given: 'jwt'
        Jwt jwt = Jwt.withTokenValue('token')
                .header("alg", "none")
                .claim(SUB, "user")
                .claim("email", "email")
                .claim("realm_access", [
                        'roles' : ['test']
                ])
                .claim("scope", "read").build()

        when:'converter is invoked'
        def result = converter.convert(jwt)

        then:
        result.authorities.size() != 0

    }
}
