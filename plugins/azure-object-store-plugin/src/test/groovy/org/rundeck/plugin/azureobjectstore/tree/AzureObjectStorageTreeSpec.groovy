package org.rundeck.plugin.azureobjectstore.tree

import com.dtolabs.rundeck.core.storage.BaseStreamResource
import com.microsoft.azure.storage.StorageUri
import com.microsoft.azure.storage.blob.BlobProperties
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlobDirectory
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.microsoft.azure.storage.blob.ListBlobItem
import org.rundeck.plugin.azureobjectstore.directorysource.AzureObjectStoreDirectAccessDirectorySource
import org.rundeck.storage.api.HasInputStream
import spock.lang.Specification

class AzureObjectStorageTreeSpec extends Specification{

    def "check upload content metadata"(){

        given:

        Map<String, String> blobMeta = [:]

        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            getMetadata()>>blobMeta
            getProperties()>>GroovyMock(BlobProperties){
                getLastModified()>>new Date()
            }
        }
        CloudBlobDirectory directory = GroovyMock(CloudBlobDirectory){

        }
        List<ListBlobItem> list = []
        list.add(blob)
        list.add(directory)
        Iterable<ListBlobItem> iterable = list
        CloudBlobContainer container = GroovyMock(CloudBlobContainer){
            getStorageUri()>> new StorageUri(new URI("http://test.com"))
            listBlobs(_)>> iterable
            getBlockBlobReference(_)>>blob
        }
        AzureObjectStoreDirectAccessDirectorySource sourceDirectory = new AzureObjectStoreDirectAccessDirectorySource()
        sourceDirectory.container = container

        String content = "abcdef"
        Map<String, String> meta = ["test":"test"]
        HasInputStream hasInputStream = Mock(HasInputStream){
            getInputStream()>> new ByteArrayInputStream(content.bytes)
        }

        BaseStreamResource baseStreamResource = new BaseStreamResource(meta,hasInputStream )

        when:
        AzureObjectStoreTree tree = new AzureObjectStoreTree(container, sourceDirectory)
        def result = tree.updateResource("/path/file",baseStreamResource)

        then:
        1*blob.setMetadata({HashMap metadata ->
            metadata == ["x_azure_meta_test":"test","x_azure_meta_Rundeck_content_size": "6"] //content length
        })
        1*blob.upload(_,_)
        result != null
        result.contents.meta!=null
        result.contents.meta.get("Rundeck-content-creation-time")!=null
        result.contents.meta.get("Rundeck-content-modify-time")!=null

    }

    def "test get resource creation time null"(){

        given:

        Map<String, String> blobMeta = ["x_azure_meta_test":"test","x_azure_meta_Rundeck_content_size": "6"]

        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            getMetadata()>>blobMeta
            getProperties()>>GroovyMock(BlobProperties){
                getLastModified()>>new Date()
            }
        }
        CloudBlobContainer container = GroovyMock(CloudBlobContainer){
            getStorageUri()>> new StorageUri(new URI("http://test.com"))
            getBlockBlobReference(_)>>blob
        }
        AzureObjectStoreDirectAccessDirectorySource sourceDirectory = new AzureObjectStoreDirectAccessDirectorySource()
        sourceDirectory.container = container

        when:
        AzureObjectStoreTree tree = new AzureObjectStoreTree(container, sourceDirectory)
        def result = tree.getResource("some/path")

        then:

        result != null
        result.contents.meta!=null
        result.contents.meta.get("Rundeck-content-creation-time")!=null
        result.contents.meta.get("Rundeck-content-modify-time")!=null
        result.contents.meta.get("test")=="test"


    }

    def "test get resource with creation time"(){

        given:

        Map<String, String> blobMeta = ["x_azure_meta_test":"test",
                                        "x_azure_meta_Rundeck_content_size": "6",
                                        "x_azure_meta_Rundeck_content_creation_time": "2022-06-12",
                                        "x_azure_meta_Rundeck_content_modify_time": "2022-06-12",

        ]

        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            getMetadata()>>blobMeta
            getProperties()>>GroovyMock(BlobProperties){
                getLastModified()>>new Date()
            }
        }
        CloudBlobContainer container = GroovyMock(CloudBlobContainer){
            getStorageUri()>> new StorageUri(new URI("http://test.com"))
            getBlockBlobReference(_)>>blob
        }
        AzureObjectStoreDirectAccessDirectorySource sourceDirectory = new AzureObjectStoreDirectAccessDirectorySource()
        sourceDirectory.container = container

        when:
        AzureObjectStoreTree tree = new AzureObjectStoreTree(container, sourceDirectory)
        def result = tree.getResource("some/path")

        then:

        result != null
        result.contents.meta!=null
        result.contents.meta.get("Rundeck-content-creation-time")=="2022-06-12"
        result.contents.meta.get("Rundeck-content-modify-time")=="2022-06-12"
        result.contents.meta.get("test")=="test"


    }
}
