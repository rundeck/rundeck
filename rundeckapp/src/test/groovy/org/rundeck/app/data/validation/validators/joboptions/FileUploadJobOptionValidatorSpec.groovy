package org.rundeck.app.data.validation.validators.joboptions

import com.dtolabs.rundeck.core.config.Features
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import org.rundeck.app.data.job.RdOption
import rundeck.services.FrameworkService
import rundeck.services.PluginService
import rundeck.services.feature.FeatureService
import spock.lang.Specification

class FileUploadJobOptionValidatorSpec extends Specification {
    def "invalid when file upload plugin feature is disabled"() {
        when:
        def fwkSvc = Mock(FrameworkService) {
            getFeatureService() >> Mock(FeatureService) {
                featurePresent(Features.FILE_UPLOAD_PLUGIN) >>  false
            }

        }
        FileUploadJobOptionValidator validator = new FileUploadJobOptionValidator(fwkSvc)
        RdOption option = new RdOption(optionType:"file")
        validator.validate(option, option.errors)
        def err = option.errors.fieldErrors[0]

        then:
        err.field == "configMap"
        err.code == "option.file.config.disabled.message"

    }

    def "invalid when file upload plugin feature is config is invalid"() {
        when:
        def fwkSvc = Mock(FrameworkService) {
            getFeatureService() >> Mock(FeatureService) {
                featurePresent(Features.FILE_UPLOAD_PLUGIN) >>  true
            }
            getPluginService() >> Mock(PluginService) {
                validatePluginConfig(_,_,_) >> new ValidatedPlugin(valid: false, report: new Validator.Report())
            }
        }
        FileUploadJobOptionValidator validator = new FileUploadJobOptionValidator(fwkSvc)
        RdOption option = new RdOption(optionType:"file")
        validator.validate(option, option.errors)
        def err = option.errors.fieldErrors[0]

        then:
        err.field == "configMap"
        err.code == "option.file.config.invalid.message"

    }
}
