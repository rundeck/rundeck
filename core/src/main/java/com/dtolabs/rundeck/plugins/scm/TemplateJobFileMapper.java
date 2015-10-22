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
        return data;
    }

    private boolean notBlank(final String groupPath) {
        return groupPath != null && !"".equals(groupPath);
    }

    private String substitute(String key, Map<String, String> data) {
        return DataContextUtils.replaceDataReferences(
                key,
                DataContextUtils.addContext(
                        "job",
                        data,
                        null
                )
        );
    }
}
