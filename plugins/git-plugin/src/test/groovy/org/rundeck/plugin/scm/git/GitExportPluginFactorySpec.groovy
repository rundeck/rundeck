package org.rundeck.plugin.scm.git

import spock.lang.Specification

/**
 * Created by greg on 8/31/15.
 */
class GitExportPluginFactorySpec extends Specification {
    def "base description"() {
        given:
        def factory = new GitExportPluginFactory()
        def desc = factory.description

        expect:
        desc.title == 'Git Export'
        desc.name == 'git-export'
        desc.properties.size() == 6
    }

    def "base description properties"() {
        given:
        def factory = new GitExportPluginFactory()
        def desc = factory.description

        expect:
        desc.properties*.name == [
                'pathTemplate',
                'url',
                'branch',
                'committerName',
                'committerEmail',
                'format'
        ]
    }

    def "setup properties without basedir"() {
        given:
        def factory = new GitExportPluginFactory()
        def properties = factory.getSetupProperties()

        expect:
        properties*.name == [
                'pathTemplate',
                'url',
                'branch',
                'committerName',
                'committerEmail',
                'format'
        ]
    }

    def "setup properties with basedir"() {
        given:
        def factory = new GitExportPluginFactory()
        def tempdir = File.createTempFile("blah", "test")
        tempdir.deleteOnExit()
        tempdir.delete()
        def properties = factory.getSetupPropertiesForBasedir(tempdir)

        expect:
        properties*.name == [
                'dir',
                'pathTemplate',
                'url',
                'branch',
                'committerName',
                'committerEmail',
                'format'
        ]
        properties.find { it.name == 'dir' }.defaultValue == new File(tempdir.absolutePath, 'scm').absolutePath
    }
}
