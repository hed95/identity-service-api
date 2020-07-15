package io.digital.patterns.identity.api.service

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.digital.patterns.identity.api.aws.AwsProperties
import io.digital.patterns.identity.api.model.CSCAMasterListRequest
import org.apache.camel.CamelContext
import org.apache.camel.ProducerTemplate
import org.apache.camel.impl.DefaultCamelContext
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.testcontainers.containers.localstack.LocalStackContainer
import spock.lang.Shared
import spock.lang.Specification

class MasterListUploadSpec extends Specification {

    @Shared
    static LocalStackContainer LOCAL_STACK =
            new LocalStackContainer().withServices(LocalStackContainer.Service.S3)


    @Shared
    static CamelContext context = new DefaultCamelContext()

    AmazonS3 amazonS3

    def setupSpec() {
        context.start()
        LOCAL_STACK.start()
    }

    def cleanupSpec() {
        LOCAL_STACK.stop()
        context.shutdown()
    }

    def properties = new AwsProperties()

    ProducerTemplate producer

    def setup() {

        final BasicAWSCredentials credentials = new BasicAWSCredentials('accessKey', 'secretAccessKey')

        amazonS3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(LOCAL_STACK
                        .getEndpointConfiguration(LocalStackContainer.Service.S3))
                .enablePathStyleAccess()
                .build()

        properties.cscaMasterListBucketName = 'csca'
        amazonS3.createBucket(properties.cscaMasterListBucketName)
        File scratchFile = File.createTempFile(UUID.randomUUID().toString(), ".txt");
        FileUtils.copyInputStreamToFile(FileUtils.getResourceAsStream("/GermanMasterList.gpg"), scratchFile);
        amazonS3.putObject(properties.cscaMasterListBucketName, 'GermanMasterList.gpg',
                scratchFile
        )

        def configuration = new CSCAMasterListRouteConfiguration(amazonS3, properties)
        configuration.gpgUserId = 'test@lodev.xyz'
        configuration.gpgUserPassword = 'testgpg'
        configuration.pgpPrivateKey = IOUtils.toString(IOUtils.getResourceAsStream("/testPrivateKey.txt"),
                "UTF-8")
        context.addRoutes(configuration.cscaRequestRoute())
        producer = context.createProducerTemplate()
    }

    def 'can process request'() {
        given: 'a request to upload a new file'
        def request = new CSCAMasterListRequest()
        request.bucketName = "csca"
        request.fileName = "GermanMasterList.gpg"

        when: 'request is submitted'
        producer.sendBody(Routes.UPDATE_CSCA_MASTER_LIST_ROUTE, request)

        then: 'master file text uploaded'
        def result = amazonS3.getObject(properties.cscaMasterListBucketName, "csca-masterlist.txt")
        result
        def asString = IOUtils.toString(result.getObjectContent(), "UTF-8")
        asString != ''
    }

}
