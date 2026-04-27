package org.rundeck.util.gui.pages.jobs.poc

import org.openqa.selenium.Dimension
import org.openqa.selenium.WebDriver
import org.rundeck.util.container.SeleniumContext
import org.rundeck.util.gui.pages.jobs.JobCreatePage
import org.rundeck.util.gui.pages.jobs.UiMode
import spock.lang.Specification

/**
 * Scenario C: verifies that the new {@code loadEditPath(String, String, UiMode)} overload
 * produces the correct URL string for each {@link UiMode} value.
 * No WebDriver or Docker required — only calls pure-string methods on the page object.
 */
class JobCreatePageUrlSpec extends Specification {

    /** Build a JobCreatePage with a minimal stub context (no real browser). */
    JobCreatePage pageWithStubContext() {
        def timeouts = Stub(WebDriver.Timeouts)
        def window   = Stub(WebDriver.Window) {
            setSize(_ as Dimension) >> {}
        }
        def manage   = Stub(WebDriver.Options) {
            window()    >> window
            timeouts()  >> timeouts
        }
        def driver   = Stub(WebDriver) {
            manage() >> manage
        }
        def context  = Stub(SeleniumContext) {
            getDriver() >> driver
            getClient() >> null
        }
        new JobCreatePage(context)
    }

    def "UiMode.DEFAULT produces no query param"() {
        given:
        def page = pageWithStubContext()

        when:
        page.loadEditPath("myProject", "abc123", UiMode.DEFAULT)

        then:
        page.loadPath == "/project/myProject/job/edit/abc123"
    }

    def "UiMode.NEXT_UI produces ?nextUi=true"() {
        given:
        def page = pageWithStubContext()

        when:
        page.loadEditPath("myProject", "abc123", UiMode.NEXT_UI)

        then:
        page.loadPath.endsWith("?nextUi=true")
    }

    def "UiMode.LEGACY produces ?legacyUi=true"() {
        given:
        def page = pageWithStubContext()

        when:
        page.loadEditPath("myProject", "abc123", UiMode.LEGACY)

        then:
        page.loadPath.endsWith("?legacyUi=true")
    }
}
