package org.rundeck.app.data.validation.validators.project

import org.rundeck.app.data.job.RdJob
import rundeck.services.FrameworkService
import spock.lang.Specification

class ProjectExistenceValidatorSpec extends Specification {
    def "Validate"() {
        when:
        def fwkSvc = Mock(FrameworkService) {
            existsFrameworkProject(_) >> projectExists
        }

        ProjectExistenceValidator validator = new ProjectExistenceValidator(fwkSvc)
        RdJob job = new RdJob()
        validator.validate("p1", job.errors)

        then:
        job.errors.errorCount == expectedErrorCount

        where:
        expectedErrorCount | projectExists
        0                  | true
        1                  | false

    }
}
