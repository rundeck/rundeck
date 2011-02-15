/**
 * Contains utility actions for API access and responses
 */
class ApiController {
    def defaultAction = "invalid"
    
    def invalid = {
        response.setStatus(404)
        request['error']=g.message(code:'api.error.invalid.request',args:[request.forwardURI])
        return error()
    }
    def renderError={
        if(flash.errorCode||request.errorCode){
            request.error=g.message(code:flash.errorCode?:request.errorCode,args:flash.errorArgs?:request.errorArgs)
        }else{
            request.error=g.message(code:"api.error.unknown")
        }
        return error()
    }

    public def success={ recall->
        return render(contentType:"text/xml",encoding:"UTF-8"){
            result(success:"true", apiversion:ApiRequestFilters.API_CURRENT_VERSION){
                recall(delegate)
            }
        }
    }

    /**
     * Utility to require specific min or max api version for an action.
     */
    public def requireVersion={min,max=0->
        if(request.api_version < min){
            request.error=g.message(code:'api.error.api-version.unsupported',
                args:[request.api_version,request.forwardURI,"Minimum supported version: "+min])
            error()
            return false
        }
        if(max>0 && request.api_version > max){
            request.error=g.message(code:'api.error.api-version.unsupported',
                args:[request.api_version,request.forwardURI,"Maximum supported version: "+max])
            error()
            return false
        }
        return true
    }

    def error={
        return render(contentType:"text/xml",encoding:"UTF-8"){
            result(error:"true", apiversion:ApiRequestFilters.API_CURRENT_VERSION){
                delegate.'error'{
                    if(flash.error){
                        message(flash.error)
                    }
                    if(request.error){
                        message(request.error)
                    }
                    if(flash.errors){
                        flash.errors.each{
                            message(it)
                        }
                    }
                    if(request.errors){
                        request.errors.each{
                            message(it)
                        }
                    }
                    if(!flash.error && !flash.errors && !request.error && !request.errors){
                        message(g.message(code:"api.error.unknown"))
                    }
                }
            }
        }
    }

    /**
     * Pass through to ScheduledExecutionController.
     * this prevents automatic link generation caused by UrlMappings file to generate links to /api in the GUI
     */
    def apiJobExport={
        return chain(controller:'scheduledExecution',action:'apiJobExport',params:params)
    }

    /**
     * Pass through to ScheduledExecutionController.
     * this prevents automatic link generation caused by UrlMappings file to generate links to /api in the GUI
     */
    def apiJobDelete={
        return chain(controller:'scheduledExecution',action:'apiJobDelete',params:params)
    }
}
