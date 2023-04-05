package org.rundeck.app.jobs.options;


import com.dtolabs.rundeck.core.jobs.options.JobOptionConfigEntry;

class JobOptionConfigPluginAttributes implements JobOptionConfigEntry {
    public static final String TYPE = "plugin-attributes";

    @Override
    String configType() {
        return TYPE
    }
    Map<String, Object> pluginAttributes

    JobOptionConfigPluginAttributes() {
        pluginAttributes = new HashMap<>()
    }

    JobOptionConfigPluginAttributes(Map<String, Object> pluginAttributes) {
        this.pluginAttributes = pluginAttributes;
    }

    void addAttribute(String key, Object value){
        pluginAttributes.put(key, value)
    }

    @Override
    Map toMap() {
        return pluginAttributes
    }
}
