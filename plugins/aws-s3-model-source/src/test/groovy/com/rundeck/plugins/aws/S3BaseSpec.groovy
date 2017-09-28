package com.rundeck.plugins.aws

import com.amazonaws.AmazonClientException
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.S3Object
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IFrameworkServices
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService
import spock.lang.Specification


class S3BaseSpec extends Specification{

    def "retrieve resource success"(){
        given:

        def nodeSet = Mock(INodeSet)
        def framework = getFramework(nodeSet)
        def amazonMock = Mock(AmazonS3){
        }
        def s3Meta = Mock(ObjectMetadata){
            getContentType() >> remoteType
        }
        def s3obj = Mock(S3Object){
            getObjectMetadata() >> s3Meta
        }

        S3Base s3 = new S3Base(bucket, filePath, extension, framework)
        s3.setAmazonS3(amazonMock)

        when:
        def result = s3.getNodes()

        then:
        1 * amazonMock.getObject(bucket,filePath) >> s3obj
        result == nodeSet

        where:
        bucket  | filePath                  | extension | remoteType
        'test'  | 'resources/resources.xml' | 'xml'     | 'application/xml'
        'test'  | 'resources/resources.yaml'| 'yaml'    | null
        'test'  | 'resources/resources.json'| 'json'    | 'application/json'

    }


    def "catch AmazonClientException and throws ResourceModelSourceException"(){
        def nodeSet = Mock(INodeSet)
        def framework = getFramework(nodeSet)
        def amazonMock = Mock(AmazonS3){
            getObject(_,_) >> {throw new AmazonClientException('')}
        }

        S3Base s3 = new S3Base(bucket, filePath, extension, framework)
        s3.setAmazonS3(amazonMock)

        when:
        s3.getNodes()

        then:
        thrown ResourceModelSourceException
        where:
        bucket  | filePath                  | extension
        'test'  | 'resources/resources.xml' | 'xml'

    }

    def "write data remote resource"(){
        given:

        def nodeSet = Mock(INodeSet)
        def framework = getFramework(nodeSet)

        def is = new ByteArrayInputStream("a".getBytes())
        S3Base s3 = new S3Base(bucket, filePath, extension, framework)
        s3.setWritable()
        def amazonMock = Mock(AmazonS3)
        s3.setAmazonS3(amazonMock)

        when:
        def result = s3.writeData(is)

        then:
        1 * amazonMock.putObject(bucket, filePath, _)
        result == 1

        where:
        bucket  | filePath                  | extension
        'test'  | 'resources/resources.xml' | 'xml'
        'test'  | 'resources/resources.yaml'| 'yaml'
        'test'  | 'resources/resources.json'| 'json'
    }

    def "write data on a read only configuration"(){
        given:

        def nodeSet = Mock(INodeSet)
        def framework = getFramework(nodeSet)

        def is = new ByteArrayInputStream("a".getBytes())
        S3Base s3 = new S3Base(bucket, filePath, extension, framework)
        def amazonMock = Mock(AmazonS3)
        s3.setAmazonS3(amazonMock)

        when:
        s3.writeData(is)

        then:
        thrown IllegalArgumentException
        0 * amazonMock.putObject(bucket, filePath, _)

        where:
        bucket  | filePath                  | extension
        'test'  | 'resources/resources.xml' | 'xml'
        'test'  | 'resources/resources.yaml'| 'yaml'
        'test'  | 'resources/resources.json'| 'json'
    }

    def "has data"(){
        def nodeSet = Mock(INodeSet)
        def framework = getFramework(nodeSet)
        def amazonMock = Mock(AmazonS3){
        }
        def s3Meta = Mock(ObjectMetadata){
            getContentType() >> remoteType
        }
        def s3obj = Mock(S3Object){
            getObjectMetadata() >> s3Meta
        }

        S3Base s3 = new S3Base(bucket, filePath, extension, framework)
        s3.setAmazonS3(amazonMock)

        when:
        def result = s3.hasData()

        then:
        1 * amazonMock.getObject(bucket,filePath) >> s3obj
        result

        where:
        bucket  | filePath                  | extension | remoteType
        'test'  | 'resources/resources.xml' | 'xml'     | 'application/xml'
        'test'  | 'resources/resources.yaml'| 'yaml'    | null
        'test'  | 'resources/resources.json'| 'json'    | 'application/json'
    }

    def "cant read file, so has no data"(){
        def nodeSet = Mock(INodeSet)
        def framework = getFramework(nodeSet)
        def amazonMock = Mock(AmazonS3){
            getObject(_,_) >> {throw new AmazonClientException('')}
        }

        S3Base s3 = new S3Base(bucket, filePath, extension, framework)
        s3.setAmazonS3(amazonMock)

        when:
        def result = s3.hasData()

        then:
        !result

        where:
        bucket  | filePath                  | extension | remoteType
        'test'  | 'resources/resources.xml' | 'xml'     | 'application/xml'
        'test'  | 'resources/resources.yaml'| 'yaml'    | null
        'test'  | 'resources/resources.json'| 'json'    | 'application/json'
    }
    


    private Framework getFramework(INodeSet nodeSet){
        def resourceFormatParser = Mock(ResourceFormatParser){
            parseDocument(_) >> nodeSet
        }
        def resourceFormatParserService = Mock(ResourceFormatParserService){
            getParserForMIMEType(_) >> resourceFormatParser
        }
        def framework = Mock(Framework){
            getResourceFormatParserService()>> resourceFormatParserService
        }
        return framework
    }

}
