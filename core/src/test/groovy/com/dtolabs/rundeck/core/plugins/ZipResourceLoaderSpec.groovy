/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.plugins

import com.dtolabs.rundeck.core.utils.FileUtils
import spock.lang.Specification

import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * @author greg
 * @since 6/22/17
 */
class ZipResourceLoaderSpec extends Specification {
    File testdir;
    File cachedir;

    def setup() throws Exception {
        testdir = new File("build/ZipResourceLoaderSpec");
        FileUtils.deleteDir(testdir);
        cachedir = new File(testdir, "cache")
        cachedir.mkdirs();
    }

    def cleanup() throws Exception {
        FileUtils.deleteDir(testdir);
    }

    def "listResources missing file"() {
        given:
        def file = new File(cachedir, "missing.zip")

        def reslist = null
        def basepath = "resources"
        def zrl = new ZipResourceLoader(cachedir, file, reslist, basepath)

        when:
        def result = zrl.listResources()
        then:
        result == []
    }

    def "listResources predefined"() {
        given:
        def file = new File(cachedir, "missing.zip")

        def reslist = ['a', 'b']
        def basepath = "resources"
        def zrl = new ZipResourceLoader(cachedir, file, reslist, basepath)

        when:
        def result = zrl.listResources()
        then:
        result == ['a', 'b']
    }

    // -------------------------------------------------------------------------
    // resolveAssetPath / openResourceStreamFor — Grails 7 manifest resolution
    // -------------------------------------------------------------------------

    def "openResourceStreamFor resolves multi-segment path via manifest"() {
        given: "manifest maps the stripped key to a hashed filename in a subdirectory"
        new File(cachedir, "manifest.properties").text =
            "menu/aclmanager.js=menu/aclmanager-abc123.js\n"
        new File(cachedir, "menu").mkdirs()
        new File(cachedir, "menu/aclmanager-abc123.js").text = "aclmanager content"

        def zrl = new ZipResourceLoader(cachedir, new File(cachedir, "dummy.zip"), null, "resources")

        when: "the original path has a leading type segment stripped by asset-pipeline"
        def stream = zrl.openResourceStreamFor("js/menu/aclmanager.js")

        then: "the hashed file is returned"
        stream.text == "aclmanager content"

        cleanup:
        stream?.close()
    }

    def "openResourceStreamFor resolves single-segment path via manifest"() {
        given: "manifest maps stripped key to hashed filename at root"
        new File(cachedir, "manifest.properties").text =
            "common.js=common-abc123.js\n"
        new File(cachedir, "common-abc123.js").text = "common content"

        def zrl = new ZipResourceLoader(cachedir, new File(cachedir, "dummy.zip"), null, "resources")

        when:
        def stream = zrl.openResourceStreamFor("js/common.js")

        then:
        stream.text == "common content"

        cleanup:
        stream?.close()
    }

    def "openResourceStreamFor falls back to original path when no manifest exists"() {
        given: "no manifest.properties in the cache dir"
        new File(cachedir, "images").mkdirs()
        new File(cachedir, "images/logo.png").bytes = [0x89, 0x50] as byte[]

        def zrl = new ZipResourceLoader(cachedir, new File(cachedir, "dummy.zip"), null, "resources")

        when:
        def stream = zrl.openResourceStreamFor("images/logo.png")

        then: "original path is used and the file is opened successfully"
        stream != null

        cleanup:
        stream?.close()
    }

    def "openResourceStreamFor falls back to original path when key not in manifest"() {
        given: "manifest exists but does not contain the requested key"
        new File(cachedir, "manifest.properties").text = "other.js=other-abc123.js\n"
        new File(cachedir, "static.js").text = "static content"

        def zrl = new ZipResourceLoader(cachedir, new File(cachedir, "dummy.zip"), null, "resources")

        when:
        def stream = zrl.openResourceStreamFor("static.js")

        then:
        stream.text == "static content"

        cleanup:
        stream?.close()
    }

    def "listResources zip basepath"() {
        given:
        def reslist = null
        def basepath = "resources"
        def file = new File(cachedir, "test.zip")
        file.withOutputStream { os ->
            new ZipOutputStream(os).with {
                putNextEntry(new ZipEntry("${basepath}/"))
                putNextEntry(new ZipEntry("${basepath}/a"))
                it << 'a data'
                closeEntry()
                putNextEntry(new ZipEntry("${basepath}/b"))
                it << 'b data'
                closeEntry()
                putNextEntry(new ZipEntry("${basepath}/c"))
                it << 'c data'
                closeEntry()
                closeEntry()
                finish()
            }
        }
        def zrl = new ZipResourceLoader(cachedir, file, reslist, basepath)

        when:
        def result = zrl.listResources()
        then:
        result == ['a', 'b', 'c']
    }
}
