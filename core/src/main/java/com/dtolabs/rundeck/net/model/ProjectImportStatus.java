package com.dtolabs.rundeck.net.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.ToString;

import java.util.List;

@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectImportStatus {
    public Boolean successful;

    @JsonProperty("import_status")
    public String importStatus;
    public List<String> errors;
    @JsonProperty("execution_errors")
    public List<String> executionErrors;
    @JsonProperty("acl_errors")
    public List<String> aclErrors;
    @JsonProperty("scm_errors")
    public List<String> scmErrors;

    public boolean getResultSuccess() {
        return null != successful ? successful : null != importStatus && "successful".equals(importStatus);
    }
}
