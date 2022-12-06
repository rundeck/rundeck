package rundeck.data.validation.validators.joboptions

import com.dtolabs.rundeck.core.common.FrameworkServiceCapabilities
import com.dtolabs.rundeck.core.config.FeatureService
import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.plugins.file.FileUploadPlugin
import groovy.util.logging.Slf4j
import org.rundeck.app.data.model.v1.job.option.OptionData
import org.springframework.validation.Errors
import org.springframework.validation.Validator

@Slf4j
class FileUploadJobOptionValidator implements Validator {
    public static final String FS_FILE_UPLOAD_PLUGIN = 'filesystem-temp'

    FrameworkServiceCapabilities frameworkService

    FileUploadJobOptionValidator(FrameworkServiceCapabilities frameworkService) {
        this.frameworkService = frameworkService
    }

    @Override
    boolean supports(Class<?> clazz) {
        return OptionData.class.isAssignableFrom(clazz)
    }

    @Override
    void validate(Object target, Errors errors) {
        OptionData opt = (OptionData)target
        if (opt.optionType == 'file') {
            if(!frameworkService.featureService.featurePresent(Features.FILE_UPLOAD_PLUGIN)) {
                errors.rejectValue(
                        'configMap',
                        'option.file.config.disabled.message',
                        "option file plugin disabled: {0}"
                )
                return
            }
            def result = frameworkService.pluginService.pluginRegistry.validatePluginByName(FS_FILE_UPLOAD_PLUGIN,
                    frameworkService.pluginService.createPluggableService(FileUploadPlugin), opt.configMap)
            if (!result.valid) {

                errors.rejectValue(
                        'configMap',
                        'option.file.config.invalid.message',
                        [result.report.toString()].toArray(),
                        "invalid file config: {0}"
                )
            }
        }
    }
}
