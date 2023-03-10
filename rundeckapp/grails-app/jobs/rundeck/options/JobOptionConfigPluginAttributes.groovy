package rundeck.options;


import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigEntry;

import java.util.Map;

class JobOptionConfigPluginAttributes implements JobOptionConfigEntry {

    private Map<String, String> pluginAttributes;
    JobOptionConfigPluginAttributes(Map<String, String> pluginAttributes) {
        this.pluginAttributes = pluginAttributes;
    }

}


