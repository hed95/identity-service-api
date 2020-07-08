package io.digital.patterns.identity.api.service

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import io.digital.patterns.identity.api.aws.AwsProperties
import io.digital.patterns.identity.api.model.MrzScan
import org.testcontainers.containers.localstack.LocalStackContainer
import spock.lang.Shared
import spock.lang.Specification

class MrzServiceSpec extends Specification {

    @Shared
    static LocalStackContainer LOCAL_STACK =
            new LocalStackContainer().withServices(LocalStackContainer.Service.S3)


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
        mrzService = new MrzService(amazonS3, properties, new ObjectMapper())
    }


    def 'can create multiple scans for same id'() {
        given: 'a mrz scan'
        def mrz = new MrzScan()
        mrz.correlationId = 'id'
        mrz.dateOfScan = new Date()
        mrz.dob = new Date().toString()
        mrz.doe = new Date().toString()
        mrz.documentNumber = 'doc'
        mrz.faceImage = 'face'
        mrz.issuingCountry = 'test'
        mrz.primaryIdentifier = 'test'
        mrz.secondaryIdentifier = 'test 2'
        mrz.mrzString = 'test'
        mrz.scanningOfficer = 'test'
        mrz.result = 'ok'

        when: 'scan is persisted'
        def id = mrzService.create(mrz)

        then: 'id is not empty'
        id != ''

        when: 'another scan with the same id is saved'
        def anotherId = mrzService.create(mrz)

        then: 'id is not empty'
        anotherId != ''
    }


    def 'can get scans for id'() {
        given: 'a mrz scan'
        MrzScan mrz = new MrzScan()
        mrz.correlationId = 'newId'
        mrz.dateOfScan = new Date()
        mrz.dob = new Date().toString()
        mrz.doe = new Date().toString()
        mrz.documentNumber = 'doc'
        mrz.faceImage = 'face'
        mrz.issuingCountry = 'test'
        mrz.primaryIdentifier = 'test'
        mrz.secondaryIdentifier = 'test 2'
        mrz.mrzString = 'test'
        mrz.scanningOfficer = 'test'
        mrz.result = 'ok'

        and: 'scan is persisted'
        mrzService.create(mrz)

        and: 'another scan'
        mrz = new MrzScan()
        mrz.correlationId = 'newId'
        mrz.dateOfScan = new Date()
        mrz.dob = new Date().toString()
        mrz.doe = new Date().toString()
        mrz.documentNumber = 'doc2'
        mrz.faceImage = 'face2'
        mrz.issuingCountry = 'test2'
        mrz.primaryIdentifier = 'test2'
        mrz.secondaryIdentifier = 'test 22'
        mrz.mrzString = 'test2'
        mrz.scanningOfficer = 'test2'
        mrz.result = 'ok'

        and: 'another scan with the same id is saved'
        mrzService.create(mrz)

        when: 'a call to get scans is made'
        def scans = mrzService.getScans('newId')

        then: 'scans should not be empty'
        scans.size() == 2

        scans.first().primaryIdentifier == 'test'
        scans.last().primaryIdentifier == 'test2'

    }
}
