package com.rundeck.plugin


class ExecutionControllerInterceptor {
    def executionModeService

    ExecutionControllerInterceptor() {
        match(controller: "execution", action: "executionMode")
    }

    boolean before() { true }

    boolean after() {

        def errors=[]

        if(!request.errors){
            if(params.activelater){

                if(!params.activeLaterValue){
                    String msg = "Active Later value must be set"
                    errors.add(msg)
                    flash.error = msg
                    request.errors=errors
                    return false
                }else{
                    if(!PluginUtil.validateTimeDuration(params.activeLaterValue)){
                        String msg = "Active Later value is not valid"
                        errors.add(msg)
                        flash.error = msg
                        request.errors=errors
                        return false
                    }
                }
            }

            if(params.passiveLater){
                if(!params.passiveLaterValue){
                    String msg = "Passive Later value must be set"
                    errors.add(msg)
                    flash.error = msg
                    request.errors=errors
                    return false
                }else{
                    if(!PluginUtil.validateTimeDuration(params.passiveLaterValue)){
                        String msg = "Passive Later value is not valid"
                        errors.add(msg)
                        flash.error = msg
                        request.errors=errors
                        return false
                    }
                }
            }
            executionModeService.saveExecutionModeLater(params)
        }

        true
    }

    void afterView() {
        // no-op
    }
}
