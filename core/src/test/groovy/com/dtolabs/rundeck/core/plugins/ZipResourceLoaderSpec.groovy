/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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
