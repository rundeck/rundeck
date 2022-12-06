package com.dtolabs.rundeck.core.options;

import org.rundeck.app.data.model.v1.job.JobData;
import org.rundeck.app.data.model.v1.job.option.OptionData;

import java.util.Map;

public interface JobOptionUrlExpander {

    /**
     * Expand the URL string's embedded property references of the form
     * ${job.PROPERTY} and ${option.PROPERTY}.  available properties are
     * limited
     */
    String expandUrl(String urlToExpand, JobData job, OptionData option, Map selectedOptsMap);
}
