package com.dtolabs.rundeck.app.internal.framework


import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import org.apache.commons.fileupload.util.Streams
import rundeck.services.ConfigurationService
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

class RundeckFilesystemProjectImporterSpec extends Specification {
    @Shared
    RundeckFilesystemProjectImporter service = new RundeckFilesystemProjectImporter()

    def setupSpec() {

    }

    def "accepts path #path for import option #option is #expect"() {
        given:
            service.importFilesOption = option
        expect:
            service.acceptsPathForImport(path) == expect
        where:
            path                            | option  | expect
            '/some/dir/'                    | null    | true
            '/some/dir/'                    | 'known' | true
            '/some/dir/'                    | 'all'   | true
            '/etc/project.properties'       | null    | false
            '/etc/project.properties'       | 'known' | false
            '/etc/project.properties'       | 'all'   | false
            '/acls/test.aclpolicy'          | null    | true
            '/acls/test.aclpolicy'          | 'known' | true
            '/acls/test.aclpolicy'          | 'all'   | true
            '/acls/test.aclpolicy.imported' | null    | false
            '/acls/test.aclpolicy.imported' | 'known' | false
            '/acls/test.aclpolicy.imported' | 'all'   | false
            '/acls/test.other'              | 'all'   | true
            '/acls/test.other'              | 'known' | false
            '/blah/blah'                    | 'known' | false
            '/blah/blah'                    | 'all'   | true
            '/readme.md'                    | 'known' | true
            '/readme.md'                    | 'all'   | true
            '/motd.md'                      | 'known' | true
            '/motd.md'                      | 'all'   | true
            '/nodes.yaml'                   | 'all'   | true
            '/nodes.yaml'                   | 'known' | true
            '/.ignored'                     | 'all'   | false
            '/sub/dir/.ignored'             | 'all'   | false
            '/sub/dir/.ignored/sub'         | 'all'   | false
            '/sub/dir/.git/sub'             | 'all'   | false
            '/sub/dir/notignored'           | 'all'   | true
            '/sub/dir/notignored'           | 'known' | false
    }

    void "mark file as imported"() {
        given:
            def other = Mock(IRundeckProject)
        when:
            service.markProjectFileAsImported(other, path)

        then:

            1 * other.loadFileResource(path, _) >> { args ->
                args[1].write('test'.bytes)
                4
            }
            1 * other.storeFileResource(
                path + '.imported', {
                def baos = new ByteArrayOutputStream()
                Streams.copy(it, baos, true)
                new String(baos.toByteArray()) == 'test'
            }
            ) >> 4
            1 * other.deleteFileResource(path) >> true

        where:
            path << ['etc/project.properties', 'readme.md', 'acls/test.aclpolicy']
    }

    void "import project from fs, no projects"() {
        given:
            def pm1 = Mock(ProjectManager) {
                1 * listFrameworkProjects() >> []
            }
            service.filesystemProjectManager = pm1
        when:
            service.importProjectsFromProjectManager()

        then:
            true
    }

    void "import project from fs, already present"() {
        given:
            def pm1 = Stub(ProjectManager) {
                listFrameworkProjects() >> [
                    Mock(IRundeckProject) {
                        _ * getName() >> 'abc'
                        //mark as imported
                        1 * loadFileResource('etc/project.properties', _) >> { args ->
                            args[1].write('test'.bytes)
                            4
                        }
                        1 * storeFileResource(
                            'etc/project.properties.imported', {
                            def baos = new ByteArrayOutputStream()
                            Streams.copy(it, baos, true)
                            new String(baos.toByteArray()) == 'test'
                        }
                        ) >> 4
                        1 * deleteFileResource('etc/project.properties') >> true
                    }
                ]
            }
            service.filesystemProjectManager = pm1
            service.projectManagerService = Mock(ProjectManager) {
                existsFrameworkProject('abc') >> true
            }
        when:
            service.importProjectsFromProjectManager()

        then:
            true
    }

    void "import project from fs, already imported"() {
        given:
            def pm1 = Mock(ProjectManager) {
                1 * listFrameworkProjects() >> [
                    Mock(IRundeckProject) {
                        _ * getName() >> 'abc'
                        1 * existsFileResource('etc/project.properties.imported') >> true
                    }
                ]
            }
            service.filesystemProjectManager = pm1
        when:
            service.importProjectsFromProjectManager()

        then:
            true
    }

    void "import project from fs, not yet imported"() {
        given:
            def projectProps = new Properties()
            projectProps['test'] = 'abc'
            def pm1 = Mock(ProjectManager) {
                1 * listFrameworkProjects() >> [
                    Mock(IRundeckProject) {
                        _ * getName() >> 'abc'
                        _ * getProjectProperties() >> projectProps
                        1 * existsFileResource('etc/project.properties.imported') >> false

                        1 * listDirPaths('') >> ['/etc/']
                        1 * listDirPaths('/etc/') >> ['/etc/project.properties']
                        //mark as imported
                        1 * loadFileResource('etc/project.properties', _) >> { args ->
                            args[1].write('test=abc'.bytes)
                            4
                        }
                        1 * storeFileResource(
                            'etc/project.properties.imported', {
                            def props = new Properties()
                            props.load(it)
                            props['test'] == 'abc'
                        }
                        ) >> 4
                        1 * deleteFileResource('etc/project.properties') >> true

                    }
                ]
            }
            service.filesystemProjectManager = pm1

            service.projectManagerService = Mock(ProjectManager)
        when:
            service.importProjectsFromProjectManager()

        then:
            1 * service.projectManagerService.existsFrameworkProject('abc') >> false
            1 * service.projectManagerService.createFrameworkProject('abc', { it.getProperty('test') == 'abc' }) >>
            Mock(IRundeckProject)
            1 * service.projectManagerService.getFrameworkProject('abc') >> Mock(IRundeckProject) {
                0 * storeFileResource(*_)
            }
    }

    @Unroll
    void "import project from fs, not yet imported with readme"() {
        given:
            def projectProps = new Properties()
            projectProps['test'] = 'abc'
            service.configurationService = Mock(ConfigurationService) {
                1 * getString('projectsStorageImportResources') >> {
                    configSet ? 'always' : null
                }
            }
            def pm1 = Stub(ProjectManager) {
                listFrameworkProjects() >> [
                    Mock(IRundeckProject) {
                        getName() >> 'abc'
                        getProjectProperties() >> projectProps
                        1 * listDirPaths('') >> ['/motd.md', '/readme.md', '/etc/']
                        1 * listDirPaths('/etc/') >> ['/etc/project.properties']
                        1 * loadFileResource('/motd.md', _) >> {
                            it[1].write('motddata'.bytes)
                            1
                        }
                        1 * loadFileResource('/readme.md', _) >> {
                            it[1].write('readmedata'.bytes)
                            1
                        }

                        1 * existsFileResource('etc/project.properties.imported') >> false

                        //mark as imported
                        1 * loadFileResource('etc/project.properties', _) >> { args ->
                            args[1].write('test=abc'.bytes)
                            4
                        }
                        1 * storeFileResource(
                            'etc/project.properties.imported', {
                            def props = new Properties()
                            props.load(it)
                            props['test'] == 'abc'
                        }
                        ) >> 4

                        1 * deleteFileResource('etc/project.properties') >> true
                        1 * deleteFileResource('/motd.md') >> true
                        1 * deleteFileResource('/readme.md') >> true
                        0 * deleteFileResource(_)
                    }
                ]
            }
            service.filesystemProjectManager = pm1

            service.projectManagerService = Mock(ProjectManager)
        when:
            service.importProjectsFromProjectManager()

        then:
            1 * service.projectManagerService.existsFrameworkProject('abc') >> projExists
            (count) * service.projectManagerService.createFrameworkProject(
                'abc',
                { it.getProperty('test') == 'abc' }
            ) >> Mock(IRundeckProject)
            1 * service.projectManagerService.getFrameworkProject('abc') >> Mock(IRundeckProject) {
                1 * storeFileResource('/motd.md', { it.text == 'motddata' }) >> 8L
                1 * storeFileResource('/readme.md', { it.text == 'readmedata' }) >> 10L
            }
        where:
            projExists | configSet | count
            false      | false     | 1
            true       | true      | 0
    }

    void "import project from fs, not yet imported with acls"() {
        given:
            def projectProps = new Properties()
            projectProps['test'] = 'abc'
            def pm1 = Stub(ProjectManager) {
                listFrameworkProjects() >> [
                    Mock(IRundeckProject) {
                        getName() >> 'abc'
                        getProjectProperties() >> projectProps
                        1 * listDirPaths('') >> ['/motd.md', '/readme.md', '/etc/', '/acls/']
                        1 * listDirPaths('/etc/') >> ['/etc/project.properties']
                        1 * listDirPaths('/acls/') >> ['/acls/test1.aclpolicy']
                        1 * loadFileResource('/motd.md', _) >> {
                            it[1].write('motddata'.bytes)
                            1
                        }
                        1 * loadFileResource('/readme.md', _) >> {
                            it[1].write('readmedata'.bytes)
                            1
                        }
                        1 * loadFileResource('/acls/test1.aclpolicy', _) >> {
                            it[1].write('acldata'.bytes)
                            1
                        }
                        1 * existsFileResource('etc/project.properties.imported') >> false

                        //mark as imported
                        1 * loadFileResource('etc/project.properties', _) >> { args ->
                            args[1].write('test=abc'.bytes)
                            4
                        }
                        1 * storeFileResource(
                            'etc/project.properties.imported', {
                            def props = new Properties()
                            props.load(it)
                            props['test'] == 'abc'
                        }
                        ) >> 4

                        1 * deleteFileResource('etc/project.properties') >> true
                        1 * deleteFileResource('/motd.md') >> true
                        1 * deleteFileResource('/readme.md') >> true
                        1 * deleteFileResource('/acls/test1.aclpolicy') >> true
                        0 * deleteFileResource(_)

                    }
                ]
            }
            service.filesystemProjectManager = pm1
            service.projectManagerService = Mock(ProjectManager)
        when:
            service.importProjectsFromProjectManager()

        then:

            1 * service.projectManagerService.existsFrameworkProject('abc') >> false
            1 * service.projectManagerService.createFrameworkProject(
                'abc',
                { it.getProperty('test') == 'abc' }
            ) >> Mock(IRundeckProject)
            1 * service.projectManagerService.getFrameworkProject('abc') >> Mock(IRundeckProject) {
                1 * storeFileResource('/motd.md', { it.text == 'motddata' }) >> 8L
                1 * storeFileResource('/readme.md', { it.text == 'readmedata' }) >> 10L
                1 * storeFileResource('/acls/test1.aclpolicy', { it.text == 'acldata' }) >> 7L
            }
    }

}
