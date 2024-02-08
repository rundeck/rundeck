package com.dtolabs.rundeck.app.api.jobs.options

import grails.compiler.GrailsCompileStatic
import grails.validation.Validateable
import io.swagger.v3.oas.annotations.media.Schema
import org.rundeck.app.data.model.v1.job.option.OptionData
import org.rundeck.app.jobs.options.JobOptionConfigRemoteUrl
import rundeck.data.validation.shared.SharedJobOptionConstraints

@GrailsCompileStatic
@Schema
class OptionValidateRequest extends OptionInput implements OptionData, Validateable {
    boolean newoption
    String valuesType
    String remoteUrlAuthenticationType
    Map configRemoteUrl
    static constraints = {
        importFrom SharedJobOptionConstraints
        valuesType(nullable: true)
        configRemoteUrl(nullable: true)
        //nb: see RemoteUrlAuthenticationType.groovy
        remoteUrlAuthenticationType(nullable: true, inList: ['BASIC', 'API_KEY', 'BEARER_TOKEN'])
    }

    JobOptionConfigRemoteUrl convertConfigRemoteUrlData() {
        def entry = this.getConfigData()?.getJobOptionEntry(JobOptionConfigRemoteUrl.TYPE)
        if (entry) {
            return entry as JobOptionConfigRemoteUrl
        } else {
            return null
        }
    }
}
