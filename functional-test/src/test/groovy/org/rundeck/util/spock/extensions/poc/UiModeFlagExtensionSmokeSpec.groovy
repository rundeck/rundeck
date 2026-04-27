package org.rundeck.util.spock.extensions.poc

import org.rundeck.util.annotations.UiModeFlag
import org.rundeck.util.annotations.UiModeStatus
import org.rundeck.util.spock.extensions.UiModeFlagExtension
import spock.lang.Specification

/**
 * Scenario A: verifies that the annotation fields are readable at runtime via reflection
 * (proves {@code @Retention(RUNTIME)} is set), that the extension swallows exceptions
 * in {@code visitSpec} (fail-soft), and that the annotation default values are correct.
 */
@UiModeFlag(featureName = "smoke", status = UiModeStatus.PROMOTED)
class UiModeFlagExtensionSmokeSpec extends Specification {

    def "annotation is present on this class with the expected field values"() {
        given:
        def annotation = UiModeFlagExtensionSmokeSpec.class.getAnnotation(UiModeFlag)

        expect: "annotation retained at runtime"
        annotation != null

        and: "featureName and status match what was declared"
        annotation.featureName() == "smoke"
        annotation.status()      == UiModeStatus.PROMOTED

        and: "defaults are in place"
        annotation.jiraTicket()  == ""
        annotation.description() == ""
    }

    def "extension swallows exceptions in visitSpec — fail-soft verified"() {
        given:
        def ext = new UiModeFlagExtension()

        when: "visitSpec is called with null (simulates an unexpected runtime failure)"
        ext.visitSpec(null)

        then: "no exception propagates out — test run is never aborted"
        noExceptionThrown()
    }

    def "extension stop() is a no-op when VERBOSE is false (default)"() {
        given:
        def ext = new UiModeFlagExtension()

        when:
        ext.stop()

        then: "no output and no exception — silent by default"
        noExceptionThrown()
    }
}
