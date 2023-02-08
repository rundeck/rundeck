package rundeck.data.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import grails.validation.Validateable
import org.rundeck.app.data.model.v1.job.config.NodeConfig
import rundeck.data.validation.shared.SharedNodeConfigConstraints

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
    Boolean nodesSelectedByDefault = true
    Boolean nodeKeepgoing=false
    Boolean doNodedispatch=false
    String  nodeRankAttribute
    Boolean nodeRankOrderAscending=true
    Boolean nodeFilterEditable = false
    Integer nodeThreadcount=1
    String  nodeThreadcountDynamic

    static constraints = {
        importFrom(SharedNodeConfigConstraints)
    }
}
