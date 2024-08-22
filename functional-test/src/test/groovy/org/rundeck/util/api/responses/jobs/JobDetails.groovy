/**
 * Differs from the Job class in the type of the options field
 */
package org.rundeck.util.api.responses.jobs

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
class JobDetails extends  JobBase {

    @JsonProperty("options")
    private List<Object> options

}
