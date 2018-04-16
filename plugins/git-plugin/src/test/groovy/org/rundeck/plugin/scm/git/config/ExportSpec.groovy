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

import org.eclipse.jgit.util.FileUtils
import spock.lang.Specification

/**
 * Created by greg on 11/8/16.
 */
class ExportSpec extends Specification {

    def "default exportUuidPreserve"() {

        given:
        Map<String, String> input = [
                dir                  : 'notnull',
                pathTemplate         : 'notnull',
                branch               : 'notnull',
                committerName        : 'notnull',
                committerEmail       : 'notnull',
                strictHostKeyChecking: 'yes',
                format               : 'xml',
                url                  : 'notnull',
                exportUuidBehavior   : eval
        ]

        when:
        def config = Config.create(Export, input)
        then:
        config.exportUuidBehavior == eval
        config.isExportPreserve()
        !config.isExportOriginal()
        !config.isExportRemove()

        where:
        eval       | _
        null       | _
        'preserve' | _
    }

    def "exportUuidBehavior remove"() {

        given:
        Map<String, String> input = [
                dir                  : 'notnull',
                pathTemplate         : 'notnull',
                branch               : 'notnull',
                committerName        : 'notnull',
                committerEmail       : 'notnull',
                strictHostKeyChecking: 'yes',
                format               : 'xml',
                url                  : 'notnull',
                exportUuidBehavior   : eval
        ]

        when:
        def config = Config.create(Export, input)
        then:
        config.exportUuidBehavior == eval
        !config.isExportPreserve()
        !config.isExportOriginal()
        config.isExportRemove()

        where:
        eval     | _
        'remove' | _
    }

    def "exportUuidBehavior original"() {

        given:
        Map<String, String> input = [
                dir                  : 'notnull',
                pathTemplate         : 'notnull',
                branch               : 'notnull',
                committerName        : 'notnull',
                committerEmail       : 'notnull',
                strictHostKeyChecking: 'yes',
                format               : 'xml',
                url                  : 'notnull',
                exportUuidBehavior   : eval
        ]

        when:
        def config = Config.create(Export, input)
        then:
        config.exportUuidBehavior == eval
        !config.isExportPreserve()
        config.isExportOriginal()
        !config.isExportRemove()

        where:
        eval       | _
        'original' | _
    }

    def "automatic synch eval"() {

        given:
        Map<String, String> input = [
                dir                  : 'notnull',
                pathTemplate         : 'notnull',
                branch               : 'notnull',
                committerName        : 'notnull',
                committerEmail       : 'notnull',
                strictHostKeyChecking: 'yes',
                format               : 'xml',
                url                  : 'notnull',
                exportUuidBehavior   : 'remove',
                pullAutomatically    : eval

        ]

        when:
        def config = Config.create(Export, input)
        then:
        config.shouldPullAutomatically() == expected

        where:
        eval        | expected
        'true'      | true
        'false'     | false
        null        | false
    }
}
