/**
 * Contains utility actions for API access and responses
 */
class ApiController {
    def defaultAction = "invalid"
    
    def invalid = {
        response.setStatus(404)
        request['error']="Invalid API Request: ${request.forwardURI}"
        return error()
    }
    def renderError={
        if(flash.errorCode||request.errorCode){
            flash.error=g.message(code:flash.errorCode?:request.errorCode,args:flash.errorArgs?:request.errorArgs)
        }else{
            flash.error=g.message(code:"api.error.unknown")
        }
        return error()
    }

    public def success={ recall->
        return render(contentType:"text/xml",encoding:"UTF-8"){
            result(success:"true"){
                recall(delegate)
            }
        }
    }

    def error={
        return render(contentType:"text/xml",encoding:"UTF-8"){
            result(error:"true"){
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
                        message("Unknown error")
                    }
                }
            }
        }
    }
}
