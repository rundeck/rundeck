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
                    if (!flash.error && !flash.errors && !request.error && !request.errors) {
                        message(g.message(code: "api.error.unknown"))
                    }
                    if(flash.error){
                        message(flash.error)
                        flash.error=null
                    }
                    if(request.error){
                        message(request.error)
                    }
                    if(flash.errors){
                        flash.errors.each{
                            message(it)
                        }
                        flash.errors = null
                    }
                    if(request.errors){
                        request.errors.each{
                            message(it)
                        }
                    }
                }
            }
        }
    }

}
