import java.lang.management.ManagementFactory

/**
 * Contains utility actions for API access and responses
 */
class ApiController {
    def defaultAction = "invalid"
    def quartzScheduler
    
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

    /**
     * API endpoints
     */

    /**
     * /api/1/system/info: display stats and info about the server
     */
    def apiSystemInfo={
        Date nowDate=new Date();
        String nodeName= servletContext.getAttribute("FRAMEWORK_NODE")
        String appVersion= grailsApplication.metadata['app.version']
        double load= ManagementFactory.getOperatingSystemMXBean().systemLoadAverage
        int processorsCount= ManagementFactory.getOperatingSystemMXBean().availableProcessors
        String osName= ManagementFactory.getOperatingSystemMXBean().name
        String osVersion= ManagementFactory.getOperatingSystemMXBean().version
        String osArch= ManagementFactory.getOperatingSystemMXBean().arch
        String vmName=ManagementFactory.getRuntimeMXBean().vmName
        String vmVendor=ManagementFactory.getRuntimeMXBean().vmVendor
        String vmVersion=ManagementFactory.getRuntimeMXBean().vmVersion
        long durationTime=ManagementFactory.getRuntimeMXBean().uptime
        Date startupDate = new Date(nowDate.getTime()-durationTime)
        int threadActiveCount=Thread.activeCount()
        return success { delegate ->
            delegate.'success' {
                message("System Stats for RunDeck ${appVersion} on node ${nodeName}")
            }
            delegate.'system'{
                timestamp(epoch:nowDate.getTime(),unit:'ms'){
                    datetime(g.w3cDateValue(date:nowDate))
                }
                rundeck{
                    version(appVersion)
                    build(grailsApplication.metadata['build.ident'])
                    node(nodeName)
                    base(servletContext.getAttribute("RDECK_BASE"))
                    apiversion(ApiRequestFilters.API_CURRENT_VERSION)
                }
                os {
                    arch(osArch)
                    name(osName)
                    version(osVersion)
                }
                jvm {
                    name(vmName)
                    vendor(vmVendor)
                    version(vmVersion)
                }
                stats{
                    uptime(duration:durationTime,unit: 'ms'){
                        since(epoch: startupDate.getTime(),unit:'ms'){
                            datetime(g.w3cDateValue(date: startupDate))
                        }
                    }
    //                errorCount('12')
//                    requestCount('12')
                    cpu{
                        loadAverage(unit:'percent',load)
                        processors(processorsCount)
                    }
                    memory(unit:'byte'){
                        max(Runtime.getRuntime().maxMemory())
                        free(Runtime.getRuntime().freeMemory())
                        total(Runtime.getRuntime().totalMemory())
                    }
                    scheduler{
                        running(quartzScheduler.getCurrentlyExecutingJobs().size())
                    }
                    threads{
                        active(threadActiveCount)
                    }
                }
            }
        }
    }
}
