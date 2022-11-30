package org.rundeck.app.data.validation.validators.execlifecycle

import com.dtolabs.rundeck.core.plugins.PluginConfigSet
import com.dtolabs.rundeck.core.plugins.SimplePluginConfiguration
import com.dtolabs.rundeck.core.plugins.ValidatedPlugin
import com.dtolabs.rundeck.core.plugins.configuration.Validator
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import org.rundeck.app.data.job.RdJob
import rundeck.services.ExecutionLifecyclePluginService
import rundeck.services.ExecutionService
import rundeck.services.FrameworkService
import rundeck.services.PluginService
import spock.lang.Specification

class ExecutionLifecyclePluginValidatorSpec extends Specification {
    def "is validate when null config"() {
        when:
        def fwkSvc = Mock(FrameworkService) {
            getExecutionService() >> Mock(ExecutionService) {
                getExecutionLifecyclePluginService() >> Mock(ExecutionLifecyclePluginService) {
                    getExecutionLifecyclePluginConfigSetForJob(_) >> null
                }
            }
        }
        ExecutionLifecyclePluginValidator validator = new ExecutionLifecyclePluginValidator(fwkSvc)
        RdJob job = new RdJob()
        job.pluginConfigMap = ["ExecutionLifecycle":null]
        validator.validate(job, job.errors)

        then:
        job.errors.errorCount == 0
    }

    def "is invalidate when execution lifecycle plugin cannot be found"() {
        when:
        def execLifecycleCfg = ["notfound-plugin":[:]]
        def fwkSvc = Mock(FrameworkService) {
            getExecutionService() >> Mock(ExecutionService) {
                getExecutionLifecyclePluginService() >> Mock(ExecutionLifecyclePluginService) {
                    getExecutionLifecyclePluginConfigSetForJob(_) >> PluginConfigSet.with(ServiceNameConstants.ExecutionLifecycle, [SimplePluginConfiguration.builder().provider("notfound-plugin").configuration(execLifecycleCfg).build()])
                }
            }
            getPluginService() >> Mock(PluginService) {
                validatePluginConfig(_,_,_,_) >> null
            }
        }
        ExecutionLifecyclePluginValidator validator = new ExecutionLifecyclePluginValidator(fwkSvc)
        RdJob job = new RdJob()
        job.pluginConfigMap = ["ExecutionLifecycle":execLifecycleCfg]
        validator.validate(job, job.errors)
        def fieldError = job.errors.fieldErrors[0]

        then:
        job.errors.errorCount == 1
        fieldError.field == "pluginConfigMap"
        fieldError.code == "scheduledExecution.executionLifecyclePlugins.pluginTypeNotFound.message"

    }

    def "is invalidate when execution lifecycle plugin field validation fails"() {
        when:
        def execLifecycleCfg = ["notfound-plugin":[:]]
        def fwkSvc = Mock(FrameworkService) {
            getExecutionService() >> Mock(ExecutionService) {
                getExecutionLifecyclePluginService() >> Mock(ExecutionLifecyclePluginService) {
                    getExecutionLifecyclePluginConfigSetForJob(_) >> PluginConfigSet.with(ServiceNameConstants.ExecutionLifecycle, [SimplePluginConfiguration.builder().provider("notfound-plugin").configuration(execLifecycleCfg).build()])
                }
            }
            getPluginService() >> Mock(PluginService) {
                validatePluginConfig(_,_,_,_) >> new ValidatedPlugin(valid: false, report: new Validator.Report())
            }
        }
        ExecutionLifecyclePluginValidator validator = new ExecutionLifecyclePluginValidator(fwkSvc)
        RdJob job = new RdJob()
        job.pluginConfigMap = ["ExecutionLifecycle":execLifecycleCfg]
        validator.validate(job, job.errors)
        def fieldError = job.errors.fieldErrors[0]

        then:
        job.errors.errorCount == 1
        fieldError.field == "pluginConfigMap"
        fieldError.code == "scheduledExecution.executionLifecyclePlugins.invalidPlugin.message"

    }
}
