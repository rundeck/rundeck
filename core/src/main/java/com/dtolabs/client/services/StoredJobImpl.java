/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* StoredJobImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Mar 24, 2010 12:36:55 PM
* $Id$
*/
package com.dtolabs.client.services;

import com.dtolabs.rundeck.core.dispatcher.IStoredJob;

/**
 * StoredJobImpl implements IStoredJob
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class StoredJobImpl implements IStoredJob {
    private String jobId;
    private String name;
    private String url;
    private String group;
    private String description;
    private String project;

    StoredJobImpl(final String jobId, final String name, final String url, final String group, final String description) {
        this.jobId = jobId;
        this.name = name;
        this.url = url;
        this.group = group;
        this.description = description;
    }

    StoredJobImpl(final String jobId, final String name, final String url, final String group, final String description,
                  final String project) {
        this.jobId = jobId;
        this.name = name;
        this.url = url;
        this.group = group;
        this.description = description;
        this.project= project;
    }

    public static IStoredJob create(final String jobId, final String name, final String url, final String group,
                                    final String description) {
        return new StoredJobImpl(jobId, name, url, group, description);
    }
    public static IStoredJob create(final String jobId, final String name, final String url, final String group,
                                    final String description, final String project) {
        return new StoredJobImpl(jobId, name, url, group, description, project);
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }
}
