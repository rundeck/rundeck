package org.rundeck.app.data.model.v1.query;

import org.rundeck.app.data.model.v1.page.Pageable;

import java.util.Map;

public interface JobQueryInputData extends Pageable {

    Map<String, Object> getInputParamMap();

    String getJobFilter();

    String getJobExactFilter();

    String getProjFilter();

    String getGroupPath();

    String getGroupPathExact();

    String getDescFilter();

    String getLoglevelFilter();

    String getIdlist();

    Boolean getScheduledFilter();

    Boolean getScheduleEnabledFilter();

    Boolean getExecutionEnabledFilter();

    String getServerNodeUUIDFilter();

    Integer getDaysAhead();

    Boolean getRunJobLaterFilter();

    default Boolean getPaginatedRequired(){return false;};
}
