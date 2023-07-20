package org.rundeck.app.data.job.schedule

import rundeck.data.job.RdJob
import spock.lang.Specification

class DefaultCrontabExpressionGeneratorSpec extends Specification {
    def "GenerateCrontab"() {
        expect:
        "0 0 0 ? * * *" == DefaultCrontabExpressionGenerator.generateCrontab(new RdJob())
    }
}
