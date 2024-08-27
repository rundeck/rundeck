/**
 * Differs from the JobDetails class in the type of the options field
 */
package org.rundeck.util.api.responses.jobs

import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class Job extends  JobBase {

    @JsonProperty("options")
    private Map<String, Object> options = new HashMap<>()

    @JsonAnySetter
    void setOptions(String key, Object value) {
        options.put(key, value);
    }

    @JsonIgnore
    Object getOptions(String key) {
        return options.get(key);
    }
}
