/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.dispatcher.DataContextUtils;
import com.dtolabs.rundeck.core.jobs.JobReference;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * A JobFileMapper using a template
 */
public class TemplateJobFileMapper implements JobFileMapper {
    String pathTemplate;
    File baseDir;

    public TemplateJobFileMapper(final String pathTemplate, final File baseDir) {
        this.pathTemplate = pathTemplate;
        this.baseDir = baseDir;
    }

    @Override
    public File fileForJob(JobReference jobReference) {
        return new File(baseDir, substitute(pathTemplate, jobReference));
    }

    @Override
    public String pathForJob(final JobReference jobReference) {
        return substitute(pathTemplate, jobReference);
    }

    private String substitute(String key, JobReference reference) {
        return substitute(key, asMap(reference));
    }

    private Map<String, String> asMap(final JobReference reference) {
        HashMap<String, String> data = new HashMap<>();
        String group = (notBlank(reference.getGroupPath()) ? (reference.getGroupPath() + '/') : "");
        data.put("project", reference.getProject());
        data.put("id", reference.getId());
        data.put("name", reference.getJobName());
        data.put("group", group);
        if (reference instanceof JobScmReference) {
            String sourceId = ((JobScmReference) reference).getSourceId();
            data.put("sourceId", sourceId != null ? sourceId : reference.getId());
        }else{
            data.put("sourceId", reference.getId());
        }
        return data;
    }

    private boolean notBlank(final String groupPath) {
        return groupPath != null && !"".equals(groupPath);
    }

    private String substitute(String key, Map<String, String> data) {
        return DataContextUtils.replaceDataReferencesInString(
                key,
                DataContextUtils.addContext(
                        "job",
                        data,
                        null
                )
        );
    }
}
