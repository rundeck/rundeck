package com.dtolabs.rundeck.core.common

import spock.lang.Specification

import java.nio.file.Files

class FrameworkProjectConfigSpec extends Specification {
    def "generateProjectPropertiesFile doesn't overwrite project.name"() {
        given:
            String projectName = 'project1'
            File destfile = Files.createTempFile('temp', '.properties').toFile()
            destfile.deleteOnExit()

            Properties orig = new Properties()
            orig.setProperty('project.name', 'project1')
            destfile.withWriter { Writer os ->
                orig.store(os, 'test')
            }

            Properties props = new Properties()
            props.setProperty('project.name', 'XXX')
            boolean merge = true
        when:
            FrameworkProjectConfig.
                generateProjectPropertiesFile(
                    projectName,
                    destfile,
                    true,
                    props,
                    merge,
                    new HashSet<String>(),
                    false
                )

            Properties result = new Properties()
            destfile.withReader {
                result.load(it)
            }
        then:
            result.getProperty('project.name') == projectName
        cleanup:
            destfile.delete()
    }
}
