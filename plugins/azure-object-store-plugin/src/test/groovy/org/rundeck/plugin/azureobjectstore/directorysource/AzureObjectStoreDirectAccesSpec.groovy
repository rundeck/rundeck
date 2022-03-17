package org.rundeck.plugin.azureobjectstore.directorysource

import com.microsoft.azure.storage.StorageUri
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlobDirectory
import com.microsoft.azure.storage.blob.CloudBlockBlob
import com.microsoft.azure.storage.blob.ListBlobItem
import org.rundeck.plugin.azureobjectstore.AzureObjectStorePlugin
import org.rundeck.plugin.azureobjectstore.directorysource.AzureObjectStoreDirectAccessDirectorySource
import spock.lang.Specification

class AzureObjectStoreDirectAccesSpec extends Specification {

    def "test checkPathExistsAndIsDirectory true"(){

        given:
        AzureObjectStoreDirectAccessDirectorySource plugin = new AzureObjectStoreDirectAccessDirectorySource()
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
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
        }

        plugin.container=container

        when:
        def result = plugin.checkPathExistsAndIsDirectory("testPath")

        then:
        result

    }

    def "test checkPathExistsAndIsDirectory no directory"(){

        given:
        AzureObjectStoreDirectAccessDirectorySource plugin = new AzureObjectStoreDirectAccessDirectorySource()
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
        }
        CloudBlockBlob blob2 = GroovyMock(CloudBlockBlob){

        }
        List<ListBlobItem> list = []
        list.add(blob)
        list.add(blob2)
        Iterable<ListBlobItem> iterable = list
        CloudBlobContainer container = GroovyMock(CloudBlobContainer){
            getStorageUri()>> new StorageUri(new URI("http://test.com"))
            listBlobs(_)>> iterable
        }

        plugin.container=container

        when:
        def result = plugin.checkPathExistsAndIsDirectory("testPath")

        then:
        !result

    }

    def "checkBlob exists"(){
        given:
        AzureObjectStoreDirectAccessDirectorySource plugin = new AzureObjectStoreDirectAccessDirectorySource()
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            exists()>>true
            getName()>>"test"
        }

        CloudBlobContainer container = GroovyMock(CloudBlobContainer){
            getStorageUri()>> new StorageUri(new URI("http://test.com"))
            getBlockBlobReference(_)>>blob
        }
        plugin.container=container

        when:

        def result = plugin.checkBlob("test")

        then:
        result

    }
    def "checkBlob doesnt exist"(){
        given:
        AzureObjectStoreDirectAccessDirectorySource plugin = new AzureObjectStoreDirectAccessDirectorySource()
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            exists()>>false
        }

        CloudBlobContainer container = GroovyMock(CloudBlobContainer){
            getStorageUri()>> new StorageUri(new URI("http://test.com"))
            getBlockBlobReference(_)>>blob
        }
        plugin.container=container

        when:

        def result = plugin.checkBlob("test")

        then:
        !result

    }

    def "checkPathExists exists"(){
        given:
        AzureObjectStoreDirectAccessDirectorySource plugin = new AzureObjectStoreDirectAccessDirectorySource()
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            exists()>>true
            getName()>>"test"
        }

        CloudBlobContainer container = GroovyMock(CloudBlobContainer){
            getStorageUri()>> new StorageUri(new URI("http://test.com"))
            getBlockBlobReference(_)>>blob
        }
        plugin.container=container

        when:

        def result = plugin.checkPathExists("test")

        then:
        result

    }
    def "checkPathExists doesnt exist"(){
        given:
        AzureObjectStoreDirectAccessDirectorySource plugin = new AzureObjectStoreDirectAccessDirectorySource()
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            exists()>>false
        }

        CloudBlobContainer container = GroovyMock(CloudBlobContainer){
            getStorageUri()>> new StorageUri(new URI("http://test.com"))
            getBlockBlobReference(_)>>blob
        }
        plugin.container=container

        when:

        def result = plugin.checkPathExists("test")

        then:
        !result

    }

    def "checkResourceExists exists"(){
        given:
        AzureObjectStoreDirectAccessDirectorySource plugin = new AzureObjectStoreDirectAccessDirectorySource()
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            exists()>>true
            getName()>>"test"
        }

        CloudBlobContainer container = GroovyMock(CloudBlobContainer){
            getStorageUri()>> new StorageUri(new URI("http://test.com"))
            getBlockBlobReference(_)>>blob
        }
        plugin.container=container

        when:

        def result = plugin.checkResourceExists("test")

        then:
        result

    }
    def "checkResourceExists doesnt exist"(){
        given:
        AzureObjectStoreDirectAccessDirectorySource plugin = new AzureObjectStoreDirectAccessDirectorySource()
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            exists()>>false
        }

        CloudBlobContainer container = GroovyMock(CloudBlobContainer){
            getStorageUri()>> new StorageUri(new URI("http://test.com"))
            getBlockBlobReference(_)>>blob
        }
        plugin.container=container

        when:

        def result = plugin.checkResourceExists("test")

        then:
        !result

    }

    def "test listEntriesAndSubDirectoriesAt 2 entries"(){

        given:
        AzureObjectStoreDirectAccessDirectorySource plugin = new AzureObjectStoreDirectAccessDirectorySource()
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
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
        }

        plugin.container=container

        when:
        def result = plugin.listEntriesAndSubDirectoriesAt("testPath")

        then:
        result.size() == 2

    }

    def "test listResourceEntriesAt 4 entries"(){

        given:
        AzureObjectStoreDirectAccessDirectorySource plugin = new AzureObjectStoreDirectAccessDirectorySource()
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
        }
        CloudBlobDirectory directory = GroovyMock(CloudBlobDirectory){

        }
        CloudBlockBlob blob2 = GroovyMock(CloudBlockBlob){
        }

        CloudBlobDirectory directory2 = GroovyMock(CloudBlobDirectory){

        }
        List<ListBlobItem> list = []
        list.add(blob)
        list.add(directory)
        list.add(blob2)
        list.add(directory2)
        Iterable<ListBlobItem> iterable = list
        CloudBlobContainer container = GroovyMock(CloudBlobContainer){
            getStorageUri()>> new StorageUri(new URI("http://test.com"))
            listBlobs(_)>> iterable
        }

        plugin.container=container

        when:
        def result = plugin.listResourceEntriesAt("testPath")

        then:
        result.size() == 2

    }

    /*def "test getBlobFile success"(){
        given:
        AzureObjectStoreDirectAccessDirectorySource plugin = new AzureObjectStoreDirectAccessDirectorySource()
        CloudBlockBlob blob = GroovyMock(CloudBlockBlob){
            exists()>>true
            getName()>>"test"
        }

        CloudBlobContainer container = GroovyMock(CloudBlobContainer){
            getStorageUri()>> new StorageUri(new URI("http://test.com"))
            getBlockBlobReference(_)>>blob
        }
        plugin.container=container

        when:

        def result = plugin.getBlobFile("test")

        then:
        result

    }*/

}
