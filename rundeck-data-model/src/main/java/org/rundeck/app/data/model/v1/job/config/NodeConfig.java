package org.rundeck.app.data.model.v1.job.config;

public interface NodeConfig {

    String getNodeInclude();
    String getNodeExclude();
    String getNodeIncludeName();
    String getNodeExcludeName();
    String getNodeIncludeTags();
    String getNodeExcludeTags();
    String getNodeIncludeOsName();
    String getNodeExcludeOsName();
    String getNodeIncludeOsFamily();
    String getNodeExcludeOsFamily();
    String getNodeIncludeOsArch();
    String getNodeExcludeOsArch();
    String getNodeIncludeOsVersion();
    String getNodeExcludeOsVersion();
    Boolean getNodeExcludePrecedence();
    Boolean getSuccessOnEmptyNodeFilter();
    Boolean getExcludeFilterUncheck();
    String  getFilter();
    String  getFilterExclude();
    Boolean getNodesSelectedByDefault();
    Boolean getNodeKeepgoing();
    Boolean getDoNodedispatch();
    Boolean getNodeRankOrderAscending();
    Boolean getNodeFilterEditable();
    Integer getNodeThreadcount();
    String  getNodeThreadcountDynamic();
    String  getNodeRankAttribute();

}
