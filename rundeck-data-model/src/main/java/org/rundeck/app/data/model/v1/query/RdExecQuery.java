package org.rundeck.app.data.model.v1.query;

import java.util.Date;
import java.util.List;

public interface RdExecQuery {
    Integer getMax();
    Integer getOffset();
    String getSortBy();
    String getSortOrder();
    String getControllerFilter();
    String getCmdFilter();
    String getGroupPathFilter();
    String getGroupPathExactFilter();
    List getExecIdFilter();
    List getExecProjects();
    Date getStartafterFilter();
    Date getStartbeforeFilter();
    Date getEndafterFilter();
    Date getEndbeforeFilter();
    boolean getDostartafterFilter();
    boolean getDostartbeforeFilter();
    boolean getDoendafterFilter();
    boolean getDoendbeforeFilter();
    String getRecentFilter();

    List<String> getJobListFilter();
    List<String> getExcludeJobListFilter();
    String getJobFilter();
    String getJobIdFilter();
    String getNodeFilter();
    String getTitleFilter();
    String getProjFilter();
    String getObjFilter();
    String getMaprefUriFilter();
    String getTypeFilter();
    String getUserFilter();
    String getMessageFilter();
    String getStatFilter();
    String getReportIdFilter();
    String getTagsFilter();
    String getAbortedByFilter();
    String getExecnodeFilter();
}
