package io.digital.patterns.identity.api.service

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.fasterxml.jackson.databind.ObjectMapper
import io.digital.patterns.identity.api.aws.AwsProperties
import io.digital.patterns.identity.api.model.Mrz
import io.digital.patterns.identity.api.model.MrzScan
import io.digital.patterns.identity.api.model.MrzType
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
