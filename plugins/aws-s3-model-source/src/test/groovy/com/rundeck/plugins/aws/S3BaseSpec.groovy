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

    def "failed retrieve resource because of wrong mimetype (only xml and json)"(){
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
        s3.getNodes()

        then:
        thrown ResourceModelSourceException
        1 * amazonMock.getObject(bucket,filePath) >> s3obj

        where:
        bucket  | filePath                  | extension | remoteType
        'test'  | 'resources/resources.xml' | 'xml'     | 'application/octet-stream'
        'test'  | 'resources/resources.json'| 'json'    | 'application/octet-stream'
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
