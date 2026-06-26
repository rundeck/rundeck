package com.rundeck.plugin

import com.dtolabs.rundeck.core.common.IRundeckProject

/**
 * Mock FrameworkService for testing
 */
class MockFrameworkService{

    String serverUUID
    IRundeckProject rundeckProject

    def projectList
    String frameworkNodeName
    Map frameworkPropertiesMap = [:]

    Map frameworkProjectsTestData = [:]

    def getFrameworkProject(String name) {
        if(rundeckProject){
            return rundeckProject
        }
        frameworkProjectsTestData[name]
    }

    IRundeckProject getRundeckProject() {
        return rundeckProject
    }

    void setRundeckProject(IRundeckProject rundeckProject) {
        this.rundeckProject = rundeckProject
    }

    def updateFrameworkProjectConfig(String project,Properties properties, Set<String> removePrefixes){
        [success:true]
    }

    boolean isClusterModeEnabled() {
        serverUUID==null?false:true
    }

    def projectNames(){
        projectList
    }

    void notifyProjectSchedulingChange(String project, boolean oldDisableExec, boolean oldDisableSched, boolean isEnabled){

    }


}

