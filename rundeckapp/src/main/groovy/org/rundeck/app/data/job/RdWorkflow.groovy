package org.rundeck.app.data.job

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.rundeck.app.data.model.v1.job.workflow.WorkflowData
import grails.validation.Validateable
import org.rundeck.app.data.validation.validators.workflow.WorkflowGrailsValidator
import rundeck.Workflow

@JsonIgnoreProperties(["errors"])
class RdWorkflow implements WorkflowData, Validateable {
    Long id
    Integer threadcount=1;
    Boolean keepgoing=false;
    List<RdWorkflowStep> steps;
    String strategy="node-first";
    Map<String,Object> pluginConfigMap;

    static constraints = {
        importFrom Workflow
        id(nullable: true)
        steps(validator: WorkflowGrailsValidator.validator)
    }
}