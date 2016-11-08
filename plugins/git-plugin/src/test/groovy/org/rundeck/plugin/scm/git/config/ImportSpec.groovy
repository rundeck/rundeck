/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package org.rundeck.plugin.scm.git.config

import spock.lang.Specification

/**
 * Created by greg on 11/8/16.
 */
class ImportSpec extends Specification {
    def "importUuidBehavior default preserve"() {
        given:
        Map<String, String> input = config(behavior)

        when:
        def config = Config.create(Import, input)
        then:
        config.importUuidBehavior == behavior
        config.isImportPreserve()
        !config.isImportArchive()
        !config.isImportRemove()

        where:
        behavior   | _
        null       | _
        'preserve' | _
    }

    def "importUuidBehavior default remove"() {
        given:
        Map<String, String> input = config(behavior)

        when:
        def config = Config.create(Import, input)
        then:
        config.importUuidBehavior == behavior
        !config.isImportPreserve()
        !config.isImportArchive()
        config.isImportRemove()

        where:
        behavior | _
        'remove' | _
    }

    def "importUuidBehavior default archive"() {
        given:
        Map<String, String> input = config(behavior)

        when:
        def config = Config.create(Import, input)
        then:
        config.importUuidBehavior == behavior
        !config.isImportPreserve()
        config.isImportArchive()
        !config.isImportRemove()

        where:
        behavior  | _
        'archive' | _
    }

    private LinkedHashMap<String, String> config(importUuidBehavior) {
        [
                dir                  : 'notnull',
                pathTemplate         : 'notnull',
                branch               : 'notnull',
                format               : 'xml',
                strictHostKeyChecking: 'yes',
                url                  : 'notnull',
                useFilePattern       : 'true',
                filePattern          : '.*\\.xml',
                importUuidBehavior   : importUuidBehavior

        ]
    }
}
