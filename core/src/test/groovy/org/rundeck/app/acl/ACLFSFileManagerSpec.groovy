package org.rundeck.app.acl

import com.dtolabs.rundeck.core.authorization.providers.BaseValidator
import com.dtolabs.rundeck.core.utils.FileUtils
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class ACLFSFileManagerSpec extends Specification {
    def "invalid filename"() {
        given:

            Path tempdir = Files.createTempDirectory("test")
            ACLFSFileManager sut = ACLFSFileManager.builder()
                                                   .directory(tempdir.toFile())
                                                   .validator(Mock(BaseValidator))
                                                   .build()
        when:
            sut.storePolicyFileContents(filename, 'asdf')

        then:
            IllegalArgumentException exc = thrown()
        cleanup:
            tempdir.deleteDir()
        where:
            filename << [
                'test',
                'aclpolicy',
                '.aclpolicy',
                '.anything.aclpolicy',
                'a/test.aclpolicy',
                '../test.aclpolicy'
            ]
    }

    def "store stream"() {
        given:
            Path tempdir = Files.createTempDirectory("test")
            ACLFSFileManager sut = ACLFSFileManager.builder()
                                                   .directory(tempdir.toFile())
                                                   .validator(Mock(BaseValidator))
                                                   .build()
        when:
            def result = sut.storePolicyFile("bob.aclpolicy", new ByteArrayInputStream('test'.bytes))

        then:
            result == 4
            def afile = new File(tempdir.toFile(), "bob.aclpolicy")
            afile.exists()
            afile.text == 'test'
        cleanup:
            FileUtils.deleteDir(tempdir.toFile())
    }

    def "store string"() {
        given:
            Path tempdir = Files.createTempDirectory("test")
            ACLFSFileManager sut = ACLFSFileManager.builder()
                                                   .directory(tempdir.toFile())
                                                   .validator(Mock(BaseValidator))
                                                   .build()
        when:
            def result = sut.storePolicyFileContents("bob.aclpolicy", 'test')

        then:
            result == 4
            def afile = new File(tempdir.toFile(), "bob.aclpolicy")
            afile.exists()
            afile.text == 'test'
        cleanup:
            FileUtils.deleteDir(tempdir.toFile())
    }

    def "delete"() {
        given:
            Path tempdir = Files.createTempDirectory("test")
            File existing = new File(tempdir.toFile(), "test.aclpolicy")
            existing << 'data'

            ACLFSFileManager sut = ACLFSFileManager.builder()
                                                   .directory(tempdir.toFile())
                                                   .validator(Mock(BaseValidator))
                                                   .build()
        when:
            def result = sut.deletePolicyFile("test.aclpolicy")

        then:
            result
            !existing.exists()
        cleanup:
            FileUtils.deleteDir(tempdir.toFile())
    }

    def "get contents"() {
        given:
            Path tempdir = Files.createTempDirectory("test")
            File existing = new File(tempdir.toFile(), "test.aclpolicy")
            existing << 'data'

            ACLFSFileManager sut = ACLFSFileManager.builder()
                                                   .directory(tempdir.toFile())
                                                   .validator(Mock(BaseValidator))
                                                   .build()
        when:
            def result = sut.getPolicyFileContents("test.aclpolicy")

        then:
            result == 'data'
        cleanup:
            FileUtils.deleteDir(tempdir.toFile())
    }

    def "load contents"() {
        given:
            Path tempdir = Files.createTempDirectory("test")
            File existing = new File(tempdir.toFile(), "test.aclpolicy")
            existing << 'data'

            ACLFSFileManager sut = ACLFSFileManager.builder()
                                                   .directory(tempdir.toFile())
                                                   .validator(Mock(BaseValidator))
                                                   .build()
            ByteArrayOutputStream out = new ByteArrayOutputStream()
        when:
            def result = sut.loadPolicyFileContents("test.aclpolicy", out)

        then:
            result == 4
            out.toString() == 'data'
        cleanup:
            FileUtils.deleteDir(tempdir.toFile())
    }

    def "get"() {
        given:
            Path tempdir = Files.createTempDirectory("test")
            File existing = new File(tempdir.toFile(), "test.aclpolicy")
            existing << 'data'

            ACLFSFileManager sut = ACLFSFileManager.builder()
                                                   .directory(tempdir.toFile())
                                                   .validator(Mock(BaseValidator))
                                                   .build()
        when:
            def result = sut.getAclPolicy("test.aclpolicy")

        then:
            result != null
            result.name == 'test.aclpolicy'
            result.created == new Date(existing.lastModified())
            result.modified == new Date(existing.lastModified())
            result.inputStream.text == 'data'
        cleanup:
            FileUtils.deleteDir(tempdir.toFile())
    }

    def "exists"() {
        given:
            Path tempdir = Files.createTempDirectory("test")
            File existing = new File(tempdir.toFile(), "test.aclpolicy")
            existing << 'data'

            ACLFSFileManager sut = ACLFSFileManager.builder()
                                                   .directory(tempdir.toFile())
                                                   .validator(Mock(BaseValidator))
                                                   .build()
        when:
            def result = sut.existsPolicyFile("test.aclpolicy")
            def result2 = sut.existsPolicyFile("other.aclpolicy")

        then:
            result
            !result2
        cleanup:
            FileUtils.deleteDir(tempdir.toFile())
    }

    def "list"() {
        given:
            Path tempdir = Files.createTempDirectory("test")
            File existing = new File(tempdir.toFile(), "test.aclpolicy")
            existing << 'data'
            File existing2 = new File(tempdir.toFile(), "monkey.aclpolicy")
            existing2 << 'data'
            File existing3 = new File(tempdir.toFile(), "monkey.aclpolic")
            existing3 << 'data'

            ACLFSFileManager sut = ACLFSFileManager.builder()
                                                   .directory(tempdir.toFile())
                                                   .validator(Mock(BaseValidator))
                                                   .build()
        when:
            def result = sut.listStoredPolicyFiles()

        then:
            result.toSet() == ['test.aclpolicy', 'monkey.aclpolicy'].toSet()
        cleanup:
            FileUtils.deleteDir(tempdir.toFile())
    }
}
