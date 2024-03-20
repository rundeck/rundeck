package org.rundeck.util.gui.pages.jobs

interface JobStep {
    String getSTEP_NAME()
    StepType getStepType()
    void configure(JobCreatePage jobCreatePage)
}