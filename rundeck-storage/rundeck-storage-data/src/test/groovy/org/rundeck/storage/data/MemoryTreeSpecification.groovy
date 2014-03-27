package org.rundeck.storage.data

import spock.lang.Specification
import org.rundeck.storage.api.ContentMeta

import static org.rundeck.storage.data.DataUtil.dataWithBytes
import static org.rundeck.storage.data.DataUtil.dataWithText
import static org.rundeck.storage.data.DataUtil.withBytes
import static org.rundeck.storage.data.DataUtil.withText

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 2/4/14
 * Time: 12:31 PM
 */
class MemoryTreeSpecification extends Specification {
    def "store data at a path"() {
        given: "basic storage"
        def storage = new MemoryTree<ContentMeta>()

        when: "store some data"
        storage.createResource("/monkey", dataWithText('blah'));
        

        then: "can retrieve data"
        storage.hasPath("/monkey")
        storage.hasResource("/monkey")
        storage.getResource("/monkey").path.name == 'monkey'
    }

    def "has root directory"() {
        given: "basic storage"
        def storage = new MemoryTree<ContentMeta>()
        expect:
        storage.hasDirectory("/")
        storage.hasPath("/")
    }

    def "has parent Directories after creating resource"() {
        given: "basic storage"
        def storage = new MemoryTree<ContentMeta>()

        when: "store some at a subpath"
        storage.createResource("/sub/path/bladdow", dataWithText('boomba'));


        then: "has parent directories"
        storage.hasDirectory("/sub/path")
        storage.hasDirectory("/sub")

        then: "has directory contents"
        storage.listDirectory("/sub/path").size() == 1
        storage.listDirectory("/sub").size() == 1

        then: "has resource contents"
        storage.listDirectoryResources("/sub/path").size() == 1
        storage.listDirectoryResources("/sub").size() == 0

        then: "has directory subdirs"
        storage.listDirectorySubdirs("/sub/path").size() == 0
        storage.listDirectorySubdirs("/sub").size() == 1

        then: "doesn't have other paths"
        !storage.hasDirectory("/subpath")
        !storage.hasDirectory("/sub/path2")

        then: "doesn't have directory at resource path"
        !storage.hasDirectory("/sub/path/bladdow")
    }

    def "deleting resource removes it"() {
        given: "basic storage"
        def storage = new MemoryTree<ContentMeta>()

        when: "store some data"
        storage.createResource("/monkey", dataWithText('blah'));
        storage.deleteResource("/monkey");

        then: "resource is gone"
        !storage.hasPath("/monkey")
    }

    def "deleting root resource does not remove root dir"() {
        given: "basic storage"
        def storage = new MemoryTree<ContentMeta>()

        when: "store a resource at root path"
        storage.createResource("/ghi", dataWithText('blah'));

        assert storage.hasDirectory("/")
        assert storage.deleteResource("/ghi");

        then: "root dir still exists"
        storage.hasPath("/")
        storage.hasDirectory("/")
    }
    def "deleting resource removes empty parent directories"() {
        given: "basic storage"
        def storage = new MemoryTree<ContentMeta>()

        when: "store some data"
        storage.createResource("/abc/def/ghi", dataWithText('blah'));

        assert storage.hasDirectory("/abc/def")
        assert storage.hasDirectory("/abc")
        assert storage.deleteResource("/abc/def/ghi");

        then: "resource parent paths are gone"
        !storage.hasPath("/abc")
        !storage.hasPath("/abc/def")
    }
    def "deleting resource leaves non-empty parent directories"() {
        given: "basic storage"
        def storage = new MemoryTree<ContentMeta>()

        when: "store some data"
        storage.createResource("/abc/def/ghi", dataWithText('blah'));

        storage.createResource("/abc/milkduds", dataWithText('blah2'));

        assert storage.hasDirectory("/abc/def")
        assert storage.hasDirectory("/abc")
        assert storage.deleteResource("/abc/def/ghi");

        then: "resource parent paths are gone"
        storage.hasPath("/abc")
        storage.hasDirectory("/abc")
        !storage.hasPath("/abc/def")
    }
}
