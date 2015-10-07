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
}
