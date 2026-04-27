package org.rundeck.util.gui.poc

import org.rundeck.util.gui.UiModes
import spock.lang.Specification

/**
 * Scenario B: verifies that {@link UiModes#defaultOnly()} produces exactly
 * 1 iteration with value {@code false} (no flag, promoted default).
 */
class UiModesDefaultOnlySpec extends Specification {

    static final UI_MODES = UiModes.defaultOnly()

    def "defaultOnly() returns exactly [[false]]"() {
        expect:
        UiModes.defaultOnly() == [[false]]
    }

    def "single iteration produces legacyUi=false (no flag needed)"() {
        expect:
        legacyUi == false

        where:
        [legacyUi] << UI_MODES
    }
}
