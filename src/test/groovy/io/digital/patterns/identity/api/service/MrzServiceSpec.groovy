package io.digital.patterns.identity.api.service

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.junit.WireMockRule
import com.github.tomjankes.wiremock.WireMockGroovy
import io.digital.patterns.identity.api.aws.AwsProperties
import io.digital.patterns.identity.api.model.Mrz
import io.digital.patterns.identity.api.model.MrzScan
import io.digital.patterns.identity.api.model.MrzType
import io.digital.patterns.identity.api.model.Workflow
import org.junit.ClassRule
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.client.RestTemplate
import org.testcontainers.containers.localstack.LocalStackContainer
import spock.lang.Shared
import spock.lang.Specification

import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.http.Response.response
import static org.springframework.security.oauth2.jwt.JwtClaimNames.SUB

class MrzServiceSpec extends Specification {

    def static wmPort = 9078

    @Shared
    static LocalStackContainer LOCAL_STACK =
            new LocalStackContainer().withServices(LocalStackContainer.Service.S3)


    @Shared
    @ClassRule
    WireMockRule wireMockRule = new WireMockRule(wmPort)

    public wireMockStub = new WireMockGroovy(wmPort)

    AmazonS3 amazonS3

    MrzService mrzService

    def setupSpec() {
        LOCAL_STACK.start()
    }

    def cleanupSpec() {
        LOCAL_STACK.stop()
    }
    def properties = new AwsProperties()

    def setup() {
        final BasicAWSCredentials credentials = new BasicAWSCredentials('accessKey', 'secretAccessKey')

        amazonS3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(LOCAL_STACK
                        .getEndpointConfiguration(LocalStackContainer.Service.S3))
                .enablePathStyleAccess()
                .build()

        properties.bucketName = 'scans'
        amazonS3.createBucket(properties.bucketName)
        mrzService = new MrzService(amazonS3, properties, new ObjectMapper(), new RestTemplate(),
                "http://localhost:9078")
    }

    def cleanup() {
        SecurityContextHolder.clearContext()
    }


    def 'can create multiple scans for same id'() {
        given: 'a mrz scan'
        def scan = new MrzScan()
        scan.correlationId = 'id'
        scan.dateOfScan = new Date()
        scan.scanningOfficer = 'test@test.com'
        scan.status = 'SUCCESS'

        scan.mrz = new Mrz()
        scan.mrz.dateOfExpiry = '27/12/2000'
        scan.mrz.dateOfBirth = '24/12/2000'
        scan.mrz.type = MrzType.TD1

        when: 'scan is persisted'
        def id = mrzService.create(scan)

        then: 'id is not empty'
        id != ''

        when: 'another scan with the same id is saved'
        def anotherId = mrzService.create(scan)

        then: 'id is not empty'
        anotherId != ''
    }

    def 'can start workflow if configured'() {
        given: 'workflow endpoint configured'
        wireMockStub.stub {
            request {
                method 'POST'
                url '/camunda/engine-rest/process-definition/key/mrzscan/start'
            }
            response {
                status: 200
                headers {
                    "Content-Type" "application/json"
                }
                body '''{
                         "id": "processInstanceId"
                        }'''
            }
        }

        and: 'a mrz scan'
        def scan = new MrzScan()
        scan.correlationId = 'id234'
        scan.dateOfScan = new Date()
        scan.scanningOfficer = 'test@test.com'
        scan.status = 'SUCCESS'
        scan.workflow = new Workflow()
        scan.workflow.processKey = 'mrzscan'
        scan.workflow.variableName = 'mrzcan'

        scan.mrz = new Mrz()
        scan.mrz.dateOfExpiry = '27/12/2000'
        scan.mrz.dateOfBirth = '24/12/2000'
        scan.mrz.type = MrzType.TD1

        and: 'authentication set up'
        Jwt jwt = Jwt.withTokenValue('token')
                .header("alg", "none")
                .claim(SUB, "user")
                .claim("email", "email")
                .claim("realm_access", [
                        'roles' : ['test']
                ])
                .claim("scope", "read").build()

        def jwtAuthentication = new JwtAuthenticationToken(jwt)
        SecurityContextHolder.getContext().setAuthentication(jwtAuthentication)

        when: 'scan is persisted'
        mrzService.create(scan)

        then: 'workflow triggered'
        verify(exactly(1), postRequestedFor(urlEqualTo("/camunda/engine-rest/process-definition/key/mrzscan/start")))

    }


    def 'can get scans for id'() {
        given: 'a mrz scan'
        MrzScan scan = new MrzScan()
        scan.correlationId = 'newId'
        scan.dateOfScan = new Date()
        scan.scanningOfficer = 'test@test.com'
        scan.status = 'SUCCESS'
        mrzService.create(scan)

        and: 'another scan'
        scan = new MrzScan()
        scan.correlationId = 'id'
        scan.dateOfScan = new Date()
        scan.scanningOfficer = 'test@test.com'
        scan.status = 'SUCCESS'
        mrzService.create(scan)

        and: 'another scan'
        scan = new MrzScan()
        scan.mrz = new Mrz()
        scan.correlationId = 'newId'
        scan.mrz.dateOfExpiry = '27/12/2000'
        scan.mrz.dateOfBirth = '24/12/2000'
        scan.mrz.type = MrzType.TD1
        mrzService.create(scan)


        when: 'a call to get scans is made'
        def scans = mrzService.getScans('newId')

        then: 'scans should not be empty'
        scans.size() == 2

    }

    def 'can delete'() {
        given: 'a mrz scan'
        MrzScan scan = new MrzScan()
        scan.correlationId = 'id'
        scan.dateOfScan = new Date()
        scan.scanningOfficer = 'test@test.com'
        scan.status = 'SUCCESS'

        scan.mrz = new Mrz()
        scan.mrz.dateOfExpiry = '27/12/2000'
        scan.mrz.dateOfBirth = '24/12/2000'
        scan.mrz.type = MrzType.TD1

        and: 'scan is persisted'
        mrzService.create(scan)

        and: 'scan is persisted'
        mrzService.create(scan)

        and: 'another scan'
        scan = new MrzScan()
        scan.correlationId = 'id'
        scan.dateOfScan = new Date()
        scan.scanningOfficer = 'test@test.com'
        scan.status = 'SUCCESS'

        scan.mrz = new Mrz()
        scan.mrz.dateOfExpiry = '27/12/2000'
        scan.mrz.dateOfBirth = '24/12/2000'
        scan.mrz.type = MrzType.TD1

        and: 'another scan with the same id is saved'
        mrzService.create(scan)


        when: 'a call to delete is made'
        mrzService.delete('newId1')

        and: 'a call then is made to get scans'
        def scans = mrzService.getScans('newId1')

        then: 'scans should be empty'
        scans.size() == 0
    }
}
