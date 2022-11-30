package org.rundeck.app.data.validation.validators.project

import org.springframework.validation.Errors
import org.springframework.validation.Validator
import rundeck.services.FrameworkService

class ProjectExistenceValidator implements Validator {

    FrameworkService frameworkService

    ProjectExistenceValidator(FrameworkService frameworkService) {
        this.frameworkService = frameworkService
    }

    @Override
    boolean supports(Class<?> clazz) {
        return String.class.isAssignableFrom(clazz)
    }

    @Override
    void validate(Object target, Errors errors) {
        if(!frameworkService.existsFrameworkProject((String)target)) {
            errors.
                    rejectValue(
                            'project',
                            'scheduledExecution.project.invalid.message',
                            [target] as Object[],
                            'Project does not exist: {0}'
                    )
        }
    }
}
