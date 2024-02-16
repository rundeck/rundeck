package org.rundeck.util.api.scm.gitea

import com.fasterxml.jackson.annotation.JsonProperty

class CreateRepoRequest {
    @JsonProperty
    String name

    @JsonProperty("default_branch")
    String defaultBranch = 'master'

    @JsonProperty("auto_init")
    Boolean autoInit = true
}
