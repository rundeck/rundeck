/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* IStoredJobsQueryImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/28/11 11:18 AM
* 
*/
package com.dtolabs.rundeck.core.dispatcher;

import java.util.*;

/**
 * IStoredJobsQueryImpl is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class IStoredJobsQueryImpl implements IStoredJobsQuery {
    private String nameMatch;
    private String groupMatch;
    private String idlist;
    private String projectFilter;

    public IStoredJobsQueryImpl(String nameMatch, String groupMatch, String idlist, String projectFilter) {
        this.nameMatch = nameMatch;
        this.groupMatch = groupMatch;
        this.idlist = idlist;
        this.projectFilter = projectFilter;
    }
    

    public String getNameMatch() {
        return nameMatch;
    }

    public void setNameMatch(String nameMatch) {
        this.nameMatch = nameMatch;
    }

    public String getGroupMatch() {
        return groupMatch;
    }

    public void setGroupMatch(String groupMatch) {
        this.groupMatch = groupMatch;
    }

    public String getIdlist() {
        return idlist;
    }

    public void setIdlist(String idlist) {
        this.idlist = idlist;
    }

    public String getProjectFilter() {
        return projectFilter;
    }

    public void setProjectFilter(String projectFilter) {
        this.projectFilter = projectFilter;
    }
}
