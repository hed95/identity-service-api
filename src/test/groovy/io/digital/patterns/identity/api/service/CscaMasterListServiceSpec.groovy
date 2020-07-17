package io.digital.patterns.identity.api.service

import com.amazonaws.auth.AWSStaticCredentialsProvider
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import io.digital.patterns.identity.api.aws.AwsProperties
import io.digital.patterns.identity.api.model.CscaMasterListUploadRequest
import org.apache.camel.CamelContext
import org.apache.camel.ProducerTemplate
import org.apache.camel.impl.DefaultCamelContext
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.testcontainers.containers.localstack.LocalStackContainer
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class CscaMasterListServiceSpec extends Specification {

    @Shared
    static LocalStackContainer LOCAL_STACK =
            new LocalStackContainer().withServices(LocalStackContainer.Service.S3)


    @Shared
    static CamelContext context = new DefaultCamelContext()

    @Shared
    static AmazonS3 amazonS3

    @Shared
    static properties = new AwsProperties()

    @Shared
    static ProducerTemplate producer

    def setupSpec() {
        final BasicAWSCredentials credentials = new BasicAWSCredentials('accessKey', 'secretAccessKey')

        context.disableJMX()
        context.start()
        LOCAL_STACK.start()

        amazonS3 = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(LOCAL_STACK
                        .getEndpointConfiguration(LocalStackContainer.Service.S3))
                .enablePathStyleAccess()
                .build()


        properties.cscaMasterListBucketName = 'csca'
        amazonS3.createBucket(properties.cscaMasterListBucketName)
        File scratchFile = File.createTempFile(UUID.randomUUID().toString(), ".ml");
        FileUtils.copyInputStreamToFile(FileUtils.getResourceAsStream("/GermanMasterList.gpg"), scratchFile);
        amazonS3.putObject(properties.cscaMasterListBucketName, 'GermanMasterList.gpg',
                scratchFile
        )

        def configuration = new CscaMasterListRouteConfiguration(amazonS3, properties)
        configuration.gpgUserId = 'test@lodev.xyz'
        configuration.gpgUserPassword = 'testgpg'

        configuration.csCaCert = IOUtils.toString(IOUtils.getResourceAsStream("/testSigningCert.pem"),
                "UTF-8")

        configuration.pgpPrivateKey = IOUtils.toString(IOUtils.getResourceAsStream("/testPrivateKey.txt"),
                "UTF-8")

        def route = configuration.cscaRequestRoute()
        context.addRoutes(route)
        producer = context.createProducerTemplate()

    }

    def cleanupSpec() {
        amazonS3.shutdown()
        LOCAL_STACK.stop()
        producer.stop()
        context.shutdown()

    }


    CscaMasterListService cscaMasterListService

    def setup() {
        cscaMasterListService = new CscaMasterListService(producer, amazonS3, properties)
    }

    def 'can process request'() {
        def conditions = new PollingConditions(timeout: 10, initialDelay: 1.5, factor: 1.25)
        given: 'a request to upload a new file'
        def request = new CscaMasterListUploadRequest()
        request.bucketName = "csca"
        request.fileName = "GermanMasterList.gpg"

        when: 'request is submitted'
        cscaMasterListService.upload(request)

        then: 'master file text uploaded'
        conditions.eventually {
            def result = amazonS3.getObject(properties.cscaMasterListBucketName, "csca-masterlist.ml")
            assert result
            def asString = IOUtils.toString(result.getObjectContent(), "UTF-8")
            assert asString != ''
        }
    }

    def 'can handle etag identifier'() {
        given: 'a master file'
        File scratchFile = File.createTempFile(UUID.randomUUID().toString(), ".ml");
        FileUtils.copyInputStreamToFile(FileUtils.getResourceAsStream("/GermanMasterList.gpg"), scratchFile)
        def objectResult = amazonS3.putObject(properties.cscaMasterListBucketName, 'csca-masterlist.ml',
                scratchFile
        )

        when: 'a request is made with the same etag'
        def result = cscaMasterListService.get(objectResult.getETag())

        then: 'content will be empty'
        result.content == null


        when: 'a request is made with a null'
        result = cscaMasterListService.get(null)

        then: 'content will not be empty'
        result.content != ''

        when: 'a request is made with an empty string'
        result = cscaMasterListService.get('')

        then: 'content will not be empty'
        result.content != ''

        when: 'a request is made with an different etag'
        result = cscaMasterListService.get('random')

        then: 'content will not be empty'
        result.content != ''

    }

}
