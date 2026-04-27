package org.rundeck.util.gui.poc

import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.gui.UiModes
import spock.lang.Specification

/**
 * Scenario B: verifies that {@link UiModes#defaultAndLegacy()} produces exactly
 * 2 iterations in the correct order and that {@code @UiModeFlag} does not alter
 * iteration count or values.
 */
@UiModeFlag(featureName = "binding-test", status = UiModeStatus.PROMOTED)
class UiModesBindingSpec extends Specification {

    static final UI_MODES = UiModes.defaultAndLegacy()

    def "defaultAndLegacy() returns exactly [[false],[true]] in order"() {
        expect:
        UiModes.defaultAndLegacy() == [[false], [true]]
    }

    def "first iteration is the promoted default (legacyUi=false), second is legacy (legacyUi=true)"() {
        expect:
        legacyUi == expectedValue

        where:
        idx | expectedValue
        0   | false
        1   | true

        and:
        [legacyUi] << UI_MODES
    }
}
