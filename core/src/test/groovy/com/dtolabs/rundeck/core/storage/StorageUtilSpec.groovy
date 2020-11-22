package com.dtolabs.rundeck.core.storage

import org.rundeck.storage.api.Path
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
    static Path p(String path){
        PathUtil.asPath path
    }
    void "delete path recursively"(){
        setup:
            def tree = Mock(Tree){
                1*hasResource(p('projects/test1')) >> false
                1*hasResource(p('projects/test1/etc')) >> false
                1*hasDirectory(p('projects/test1')) >> true
                1*hasDirectory(p('projects/test1/etc')) >> true
                1*deleteResource(p('projects/test1/file1')) >> true
                1*deleteResource(p('projects/test1/file2')) >> true
                1*deleteResource(p('projects/test1/etc/project.properties')) >> true
                1*listDirectory(p('projects/test1')) >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>p('projects/test1/file1')
                    },
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>p('projects/test1/file2')
                    },
                    Stub(Resource){
                        isDirectory()>>true
                        getPath()>>p('projects/test1/etc')
                    }
                ]
                1*listDirectory(p('projects/test1/etc')) >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>p('projects/test1/etc/project.properties')
                    }
                ]
            }
        when:
           def result= StorageUtil.deletePathRecursive(tree, p('projects/test1'))
        then:
            result

    }
}
