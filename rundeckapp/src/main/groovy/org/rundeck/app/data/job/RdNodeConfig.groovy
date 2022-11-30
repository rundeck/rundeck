package org.rundeck.app.data.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.job.config.NodeConfig
import rundeck.ScheduledExecution

@JsonIgnoreProperties(["errors"])
class RdNodeConfig implements NodeConfig, Validateable {
    String nodeInclude
    String nodeExclude
    String nodeIncludeName
    String nodeExcludeName
    String nodeIncludeTags
    String nodeExcludeTags
    String nodeIncludeOsName
    String nodeExcludeOsName
    String nodeIncludeOsFamily
    String nodeExcludeOsFamily
    String nodeIncludeOsArch
    String nodeExcludeOsArch
    String nodeIncludeOsVersion
    String nodeExcludeOsVersion
    Boolean nodeExcludePrecedence=true
    Boolean successOnEmptyNodeFilter=false
    String filter
    String filterExclude
    Boolean excludeFilterUncheck = false

    static constraints = {
        importFrom(ScheduledExecution)
    }
}
