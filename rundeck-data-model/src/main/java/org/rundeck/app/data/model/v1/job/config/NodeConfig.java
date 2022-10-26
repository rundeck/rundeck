package org.rundeck.app.data.model.v1.job.config;

public interface NodeConfig {
    Boolean isSelectedByDefault();
    Boolean isKeepgoing();
    Boolean isDoNodedispatch();
    Boolean isRankOrderAscending();
    Boolean isFilterEditable();
    Boolean isExcludePrecedence();
    Boolean isSuccessOnEmptyNodeFilter();
    Boolean isExcludeFilterUncheck();
    String  getRankAttribute();
    String  getRankOrder();
    String  getThreadcountDynamic();
    String  getFilter();
    String  getFilterExclude();
    Integer getThreadcount();
}
