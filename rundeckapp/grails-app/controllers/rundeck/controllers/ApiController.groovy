package rundeck.controllers

import java.lang.management.ManagementFactory
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.server.authorization.AuthConstants
import rundeck.filters.ApiRequestFilters

/**
 * Contains utility actions for API access and responses
 */
class ApiController {
    def defaultAction = "invalid"
    def quartzScheduler
    def frameworkService
    
    def invalid = {
        response.setStatus(404)
        request['error']=message(code:'api.error.invalid.request',args:[request.forwardURI])
        return error()
    }
    def renderError={
        if (flash.responseCode) {
            response.setStatus(flash.responseCode)
        }
        if(flash.errorCode||request.errorCode){
            request.error=message(code:flash.errorCode?:request.errorCode,args:flash.errorArgs?:request.errorArgs)
        }else{
            request.error=message(code:"api.error.unknown")
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
            response.setStatus(400)
            request.error=message(code:'api.error.api-version.unsupported',
                args:[request.api_version,request.forwardURI,"Minimum supported version: "+min])
            request.apiErrorCode="api-version-unsupported"
            error()
            return false
        }
        if(max>0 && request.api_version > max){
            response.setStatus(400)
            request.error=message(code:'api.error.api-version.unsupported',
                args:[request.api_version,request.forwardURI,"Maximum supported version: "+max])
            request.apiErrorCode = "api-version-unsupported"
            error()
            return false
        }
        return true
    }

    def error={
        return render(contentType:"text/xml",encoding:"UTF-8"){
            result(error:"true", apiversion:ApiRequestFilters.API_CURRENT_VERSION){
                def errorprops=[:]
                if(request.apiErrorCode){
                    errorprops=[code:request.apiErrorCode]
                }
                delegate.'error'(errorprops){
                    if (!flash.error && !flash.errors && !request.error && !request.errors) {
                        delegate.'message'(message(code: "api.error.unknown"))
                    }
                    if(flash.error){
                        delegate.'message'(flash.error)
                        flash.error=null
                    }
                    if(request.error){
                        delegate.'message'(request.error)
                    }
                    if(flash.errors){
                        flash.errors.each{
                            delegate.'message'(it)
                        }
                        flash.errors = null
                    }
                    if(request.errors){
                        request.errors.each{
                            delegate.'message'(it)
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
        Framework framework = frameworkService.getFrameworkFromUserSession(session, request)
        if (!frameworkService.authorizeApplicationResource(framework, [type: 'resource', kind: 'system'], AuthConstants.ACTION_READ)) {
            response.setStatus(403)
            request.error = message(code: 'api.error.item.unauthorized', args: ['Read System Info', 'Rundeck', ""])
            return error()
        }
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
                delegate.'message'("System Stats for RunDeck ${appVersion} on node ${nodeName}")
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
