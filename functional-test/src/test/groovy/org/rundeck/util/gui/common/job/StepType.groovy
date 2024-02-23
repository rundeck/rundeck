package org.rundeck.util.gui.common.job

enum StepType {

    NODE ("data-node-step-type"),
    WORKFLOW ("data-step-type");

    private String stepType

    StepType(String stepType){
        this.stepType = stepType
    }

    String getStepType(){
        return this.stepType
    }

}