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

package org.rundeck.plugin.scm.git

import spock.lang.Specification

/**
 * Created by greg on 10/5/15.
 */
class RenameTrackerSpec extends Specification {
    def "trackItem"() {
        given:
        def t = new RenameTracker<String>()

        when:
        t.trackItem("a", "b")

        then:
        t.wasRenamed("a")
        t.renamedValue("a") == "b"
        t.originalValue("b") == "a"
    }
    def "trackItem with revert"() {
        given:
        def t = new RenameTracker<String>()

        when:
        t.trackItem("a", "b")
        t.trackItem("b", "a")

        then:
        !t.wasRenamed("a")
        t.renamedValue("a") == null
        t.originalValue("b") == null
    }

    def "trackItem invalid"() {
        given:
        def t = new RenameTracker<String>()

        when:
        t.trackItem("a", "a")

        then:
        !t.renamedTrackedItems['a']
    }
}
