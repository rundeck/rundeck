package org.rundeck.app.providers

import grails.testing.gorm.DataTest
import org.rundeck.app.data.model.v1.project.SimpleProjectBuilder
import org.rundeck.app.data.model.v1.storage.RundeckStorage
import org.rundeck.app.data.model.v1.storage.SimpleStorageBuilder
import org.rundeck.app.data.providers.GormProjectDataProvider
import org.rundeck.app.data.providers.storage.GormStorageDataProvider
import org.rundeck.spi.data.DataAccessException
import org.rundeck.storage.api.PathUtil
import rundeck.Project
import rundeck.Storage
import rundeck.services.data.ProjectDataService
import rundeck.services.data.StorageDataService
import spock.lang.Specification
import spock.lang.Unroll

import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertNull
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertTrue

class GormStorageDataProviderSpec extends Specification implements DataTest{
    GormStorageDataProvider provider = new GormStorageDataProvider()

    void setup() {
        mockDomains(Storage)
        mockDataService(StorageDataService)
        provider.storageDataService = applicationContext.getBean(StorageDataService)

    }

    def "Get Data"() {
        given:
        def storage1 = new Storage(namespace: 'other', data: 'abc1'.bytes, name: 'abc', dir: 'def',
                storageMeta: [abc: 'xyz1']).save(flush:true)

        when:
        RundeckStorage res = provider.getData(storage1.id)

        then:
        res.getName() == 'abc'
        res.getDir() ==  'def'
        res.getNamespace() == 'other'
        res.getData() == 'abc1'.bytes
    }

    def "Create"() {

        when:
        SimpleStorageBuilder data =  new SimpleStorageBuilder()
                .setData('abc'.bytes)
                .setDir("xyz")
                .setName('abc')
                .setJsonData(RundeckStorage.storageMetaAsString([abc: 'xyz']))
        provider.create(data)

        then:
        provider.findResource(null, null,'xyz') == null
        provider.findResource(null,'xyz','abc') != null
    }

    def "Update"() {

        when:
        SimpleStorageBuilder data =  new SimpleStorageBuilder()
                .setData('abc'.bytes)
                .setDir("xyz")
                .setName('abc')
                .setJsonData(RundeckStorage.storageMetaAsString([abc: 'xyz']))

        Long storageId = provider.create(data)

        def storageName = provider.getData(storageId).getName()
        data.setName("updated")
        provider.update(storageId, data)
        def storage1 = provider.getData(storageId)

        then:
        storageName == 'abc'
        storage1.name == 'updated'
    }
    def "DeleteResource ok"() {
        when:
        def storage = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(flush:true)
        new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush:true)

        provider.delete(storage.id)
        def store1 = Storage.findByNamespaceAndDirAndName(null,'', 'abc')
        then:
        store1 == null
        Storage.get(storage.id) == null
    }

    def "list subdirs"() {
        given:
        def storage1 = new Storage(data: 'abc1'.bytes, name: 'abc', dir: '', storageMeta: [abc: 'xyz1']).save(flush: true)
        new Storage(data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush: true)
        new Storage(data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(flush: true)
        new Storage(data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(flush: true)
        new Storage(data: 'abc3'.bytes, name: 'def', dir: 'zinc/pyx',
                storageMeta: [abc: 'xyz3']).save(flush: true)
        when:
        def res1 = provider.listDirectorySubdirs(null, PathUtil.asPath(''))
        then:
        res1.size() == 5
    }

    def "list Directory"() {
        when:
        def storage1 = new Storage(namespace: 'other', data: 'abc1'.bytes, name: 'abc', dir: '',
                storageMeta: [abc: 'xyz1']).save(flush:true)
        new Storage(namespace: 'other',data: 'abc2'.bytes, name: 'abc', dir: 'xyz', storageMeta: [abc: 'xyz2']).save(flush:true)
        new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'abc3', dir: 'xyz', storageMeta: [abc: 'xyz3']).save(flush:true)
        new Storage(namespace: 'other',data: 'abc3'.bytes, name: 'banana.gif', dir: 'xyz/monkey/tree',
                storageMeta: [abc: 'xyz3']).save(flush:true)
        def res1 = provider.listDirectory(null, PathUtil.asPath(''))
        def res2 = provider.listDirectory('other',PathUtil.asPath(''))
        then:
        res1 != null
        res1.size() == 0
        res2 != null
        res2.size() == 4
    }
}
