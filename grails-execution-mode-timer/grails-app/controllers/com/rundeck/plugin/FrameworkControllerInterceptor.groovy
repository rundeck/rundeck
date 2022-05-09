package com.rundeck.plugin


class FrameworkControllerInterceptor {

    int order = HIGHEST_PRECEDENCE + 106
    def frameworkService
    def updateModeProjectService

    FrameworkControllerInterceptor() {
        match(controller: "framework", action: "saveProject")
    }

    boolean before() { true }

    boolean after() {

         if(params.cancel == "Cancel"){
             return true
         }

         if(!request.errors){
            def project=params.project
            Properties projProps = new Properties()

            //load extra configuration for grails services
            def pconfigurable = frameworkService.validateProjectConfigurableInput(
                    params.extraConfig,
                    'extraConfig.',
                    { String category -> category != 'resourceModelSource' },
            )
            if (pconfigurable.props) {
                projProps.putAll(pconfigurable.props)
            }

            boolean saveStatus = updateModeProjectService.saveExecutionLaterSettings(project, projProps)
        }

        true

    }

    void afterView() {
        // no-op
    }
}
