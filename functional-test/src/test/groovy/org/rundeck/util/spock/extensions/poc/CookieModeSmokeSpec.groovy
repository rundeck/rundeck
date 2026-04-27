package org.rundeck.util.spock.extensions.poc

import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiMechanism
import org.rundeck.util.annotations.UiModeStatus
import spock.lang.Specification

/**
 * Scenario D: verifies that the {@code mechanism} field is retained at runtime,
 * readable via reflection, and correctly threaded end-to-end from annotation
 * declaration to the value the extension would read.
 */
@UiModeFlag(featureName = "cookie-test", mechanism = UiMechanism.COOKIE, status = UiModeStatus.NEXT_UI)
class CookieModeSmokeSpec extends Specification {

    def "annotation on this class has mechanism=COOKIE readable at runtime"() {
        given:
        def annotation = CookieModeSmokeSpec.class.getAnnotation(UiModeFlag)

        expect: "annotation survived to runtime (RUNTIME retention works)"
        annotation != null

        and: "mechanism field carries COOKIE"
        annotation.mechanism() == UiMechanism.COOKIE

        and: "other fields are correct"
        annotation.featureName() == "cookie-test"
        annotation.status()      == UiModeStatus.NEXT_UI
    }

    def "UiMechanism enum has exactly the expected constants"() {
        expect: "no undeclared values that would break the scanner"
        UiMechanism.values().toList() == [UiMechanism.URL_PARAM, UiMechanism.COOKIE]
    }
}
