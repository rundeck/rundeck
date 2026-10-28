package org.rundeck.util.gui.pages.project

import org.openqa.selenium.By
import org.openqa.selenium.WebElement

/**
 * Job stats section shared by the job show and execution show pages
 */
trait JobStatsTrait {
    abstract List<WebElement> els(By by)

    static By nextUiJobStatsBy = By.cssSelector('#_job_stats_main .vue-ui-socket')
    static By currentUiJobStatsBy = By.cssSelector('#_job_stats_main .jobstats')

    /**
     * @return true if the Next UI socket placeholder is rendered in the job stats section
     */
    boolean hasNextUiJobStats() {
        !els(nextUiJobStatsBy).isEmpty()
    }

    /**
     * @return true if the Current UI stats markup is rendered in the job stats section
     */
    boolean hasCurrentUiJobStats() {
        !els(currentUiJobStatsBy).isEmpty()
    }
}
