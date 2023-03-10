package rundeck.options;


import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigEntry;

import java.util.Map;

public class JobOptionConfigPluginAttributes implements JobOptionConfigEntry {

    private Map<String, String> pluginAttributes;
    public JobOptionConfigPluginAttributes(Map<String, String> pluginAttributes) {
        this.pluginAttributes = pluginAttributes;
    }

    public Map<String, String> getPluginAttributes() {
        return pluginAttributes;
    }

    public void setPluginAttributes(Map<String, String> pluginAttributes) {
        this.pluginAttributes = pluginAttributes;
    }

}


