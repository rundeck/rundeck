package com.dtolabs.rundeck.core.utils

import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.util.zip.ZipEntry

class ZipUtilSpec extends Specification {
    @Unroll
    def "extract entry outside of destination dir"() {
        given:
        File destDir = Files.createTempDirectory("test").toFile()
        Enumeration<ZipEntry> entries = new Vector<ZipEntry>(
            [
                new ZipEntry(path)
            ]
        ).elements()

        when:
        ZipUtil.extractZip(entries, null, destDir, null, null, null)

        then:
        IOException e = thrown()
        e.message.contains("Path is outside of destination directory")
        where:
        path     | _
        '../xyz' | _
        '../../../../../../../../../../../../../../../../../../../../xyz' | _
    }
}
