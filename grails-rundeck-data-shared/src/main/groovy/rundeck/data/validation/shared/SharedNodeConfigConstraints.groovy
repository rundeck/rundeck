package rundeck.data.validation.shared

import grails.validation.Validateable

class SharedNodeConfigConstraints implements Validateable {
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
    Boolean nodeExcludePrecedence
    Boolean successOnEmptyNodeFilter
    String filter
    String filterExclude
    Boolean excludeFilterUncheck
    Boolean nodesSelectedByDefault
    Boolean nodeKeepgoing
    Boolean doNodedispatch
    String  nodeRankAttribute;
    Boolean nodeRankOrderAscending
    Boolean nodeFilterEditable
    Integer nodeThreadcount
    String  nodeThreadcountDynamic

    static constraints = {
        nodeInclude(nullable:true)
        nodeExclude(nullable:true)
        nodeIncludeName(nullable:true)
        nodeExcludeName(nullable:true)
        nodeIncludeTags(nullable:true)
        nodeExcludeTags(nullable:true)
        nodeIncludeOsName(nullable:true)
        nodeExcludeOsName(nullable:true)
        nodeIncludeOsFamily(nullable:true)
        nodeExcludeOsFamily(nullable:true)
        nodeIncludeOsArch(nullable:true)
        nodeExcludeOsArch(nullable:true)
        nodeIncludeOsVersion(nullable:true)
        nodeExcludeOsVersion(nullable:true)
        nodeExcludePrecedence(nullable:true)
        successOnEmptyNodeFilter(nullable: true)
        filter(nullable:true)
        filterExclude(nullable: true)
        excludeFilterUncheck(nullable: true)
        nodeKeepgoing(nullable:true)
        doNodedispatch(nullable:true)
        nodeRankOrderAscending(nullable:true)
        nodeRankAttribute(nullable:true)
        nodeFilterEditable(nullable: true)
        nodeThreadcountDynamic(nullable: true)
        nodeThreadcount(nullable:true)
        nodesSelectedByDefault(nullable: true)
    }
}
