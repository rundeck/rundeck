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

package rundeck

import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 11/4/15.
 */
class OptionSpec extends Specification {
    @Unroll("path #path valid #value")
    def "validate default storage path"() {
        given:
        def opt = new Option(name: 'bob', defaultStoragePath: path)
        expect:
        value == opt.validate(['defaultStoragePath'])

        where:
        path             | value
        'keys/blah'      | true
        'keys/blah/blah' | true
        'keys/'          | false
        'blah/toodles'   | false
    }

    def "from map default storage path"() {
        given:
        def opt = Option.fromMap('test', datamap)

        expect:
        value == opt.defaultStoragePath

        where:
        datamap                   | value
        [:]                       | null
        [storagePath: 'keys/abc'] | 'keys/abc'

    }

    def "to map default storage path"() {
        given:
        def opt = new Option(name: 'bob', defaultStoragePath: path)
        expect:
        path == opt.toMap().storagePath

        where:
        path       | _
        'keys/abc' | _
        null       | _
    }
}
