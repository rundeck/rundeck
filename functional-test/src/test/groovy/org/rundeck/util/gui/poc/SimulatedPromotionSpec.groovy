package org.rundeck.util.gui.poc

import org.rundeck.util.gui.UiModes
import spock.lang.Specification

/**
 * Scenario F post-promotion: legacy branch removed, single-mode only.
 * Demonstrates the cleanup diff: @UiModeFlag removed, if(legacyUi) branch removed,
 * UI_MODES switched to defaultOnly().
 */
class SimulatedPromotionSpec extends Specification {

    static final UI_MODES = UiModes.defaultOnly()

    def "simulated feature test runs in default mode only"() {
        expect:
        simulateClick()

        where:
        [_] << UI_MODES
    }

    private boolean simulateClick() {
        defaultPath()
    }

    private boolean defaultPath() { true }
}
