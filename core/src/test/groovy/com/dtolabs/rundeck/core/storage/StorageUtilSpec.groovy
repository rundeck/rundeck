package com.dtolabs.rundeck.core.storage

import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.Tree
import spock.lang.Specification

class StorageUtilSpec extends Specification {
    def "parse date null value should return null"() {
        given:
            def content = StorageUtil.withStream(new ByteArrayInputStream(''.bytes), [:])
        expect:
            content.modificationTime == null
            content.creationTime == null
    }

    def "parse date result"() {
        given:
            Date date = new Date(39393939000)
            Date date2 = new Date(12312312000)
            def content = StorageUtil.withStream(
                new ByteArrayInputStream(''.bytes), [
                (StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME)  : StorageUtil.formatDate(date),
                (StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME): StorageUtil.formatDate(date2),
            ]
            )
        expect:
            content.modificationTime == date
            content.creationTime == date2
    }

    def "content length null value should return null"() {
        given:
            def content = StorageUtil.withStream(new ByteArrayInputStream(''.bytes), [:])
        expect:
            content.contentLength == -1
    }

    void " delete path recursively"(){
        setup:
            def tree = Stub(Tree){
                1*hasResource("projects/test1") >> false
                1*hasResource("projects/test1/etc") >> false
                1*hasDirectory("projects/test1") >> true
                1*hasDirectory("projects/test1/etc") >> true
                1*deleteResource("projects/test1/file1") >> true
                1*deleteResource("projects/test1/file2") >> true
                1*deleteResource("projects/test1/etc/project.properties") >> true
                1*listDirectory("projects/test1") >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("projects/test1/file1")
                    },
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("projects/test1/file2")
                    },
                    Stub(Resource){
                        isDirectory()>>true
                        getPath()>>PathUtil.asPath("projects/test1/etc")
                    }
                ]
                1*listDirectory("projects/test1/etc") >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("projects/test1/etc/project.properties")
                    }
                ]
            }
        when:
           def result= StorageUtil.deletePathRecursive(tree, PathUtil.asPath('projects/test1'))
        then:
            result

    }
}
