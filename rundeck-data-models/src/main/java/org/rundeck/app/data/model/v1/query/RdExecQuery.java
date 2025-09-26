/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
