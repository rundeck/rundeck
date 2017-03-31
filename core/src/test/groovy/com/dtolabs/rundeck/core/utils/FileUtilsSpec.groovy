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

package com.dtolabs.rundeck.core.utils

import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author greg
 * @since 3/31/17
 */
class FileUtilsSpec extends Specification {
    @Unroll
    def "get common prefix"() {
        given:

        when:
        def result = FileUtils.getCommonPrefix(files)

        then:
        result == expectpath

        where:
        files                                                     | expectpath
        ['/a/b/c', '/a/x/y', '/a/b/x']                            | '/a/'
        ['/a/b/c', '/a/b/y', '/a/b/x']                            | '/a/b/'
        ['/a/b/c', '/a/c/y', '/d/b/x']                            | '/'
        ['/a/b/c', '/a/b/c/something', '/a/b/c/da']               | '/a/b/c'
        ['/a/b/c/something', '/a/b/c', '/a/b/c/something/else']   | '/a/b/c'
        ['C:\\a\\path\\dos', '/a/b/c', '/a/b/c/something/else']   | ''
        ['C:\\a\\path\\dos', 'C:\\a\\path', 'C:\\a\\nother\\dos'] | 'C:\\a\\'
    }

}
