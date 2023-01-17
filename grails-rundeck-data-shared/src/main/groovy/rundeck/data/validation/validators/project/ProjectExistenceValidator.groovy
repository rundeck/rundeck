package rundeck.data.validation.validators.project

import org.rundeck.app.core.FrameworkServiceCapabilities
import org.springframework.validation.Errors
import org.springframework.validation.Validator

class ProjectExistenceValidator implements Validator {

    FrameworkServiceCapabilities frameworkService

    ProjectExistenceValidator(FrameworkServiceCapabilities frameworkService) {
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
