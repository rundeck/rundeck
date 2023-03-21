package rundeck.options;


import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigEntry;

class JobOptionConfigPluginAttributes implements JobOptionConfigEntry {

    Map<String, String> pluginAttributes;

    JobOptionConfigPluginAttributes() {
        pluginAttributes = new HashMap<>()
    }

    JobOptionConfigPluginAttributes(Map<String, String> pluginAttributes) {
        this.pluginAttributes = pluginAttributes;
    }

    void addAttribute(String key, String value){
        pluginAttributes.put(key, value)
    }
}
