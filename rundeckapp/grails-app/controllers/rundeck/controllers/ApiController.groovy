package rundeck.controllers

import com.dtolabs.rundeck.core.authorization.AuthContext

import javax.servlet.http.HttpServletResponse
import java.lang.management.ManagementFactory

import com.dtolabs.rundeck.server.authorization.AuthConstants
import rundeck.filters.ApiRequestFilters

/**
 * Contains utility actions for API access and responses
 */
class ApiController {
    def defaultAction = "invalid"
    def quartzScheduler
    def frameworkService
    def apiService

    def invalid = {
        return apiService.renderErrorXml(response,[code:'api.error.invalid.request',args:[request.forwardURI],status:HttpServletResponse.SC_NOT_FOUND])
    }

    /**
     * API endpoints
     */
    /**
     * Return true if grails configuration allows given feature, or '*' features
     * @param name
     * @return
     */
    private boolean featurePresent(def name){
        def splat=grailsApplication.config.feature?.incubator?.getAt('*') in ['true',true]
        return splat || (grailsApplication.config.feature?.incubator?.getAt(name) in ['true',true])
    }
    /**
     * Set an incubator feature toggle on or off
     * @param name
     * @param enable
     */
    private void toggleFeature(def name, boolean enable){
        grailsApplication.config.feature?.incubator?.putAt(name, enable)
    }
    /**
     * Feature toggle api endpoint for development mode
     */
    def featureToggle={
        def respond={
            render(contentType: 'text/plain', text: featurePresent(params.featureName) ? 'true' : 'false')
        }
        if(params.featureName){
            if(request.method=='GET'){
                respond()
            } else if (request.method=='PUT'){
                toggleFeature(params.featureName, request.inputStream.text=='true')
                respond()
            }else if(request.method=='POST'){
                toggleFeature(params.featureName, true)
                respond()
            }else if(request.method=='DELETE'){
                toggleFeature(params.featureName, false)
                respond()
            }
        }else{
            render(contentType: 'text/plain'){
                grailsApplication.config.feature?.incubator?.each{k,v->
                    out<<"${k}:${v in [true,'true']}\n"
                }
            }
        }
    }

    /**
     * /api/1/system/info: display stats and info about the server
     */
    def apiSystemInfo={
        AuthContext authContext = frameworkService.getAuthContextForSubject(session.subject)
        if (!frameworkService.authorizeApplicationResource(authContext, AuthConstants.RESOURCE_TYPE_SYSTEM,
                AuthConstants.ACTION_READ)) {
            return apiService.renderErrorXml(response,[status:HttpServletResponse.SC_FORBIDDEN, code: 'api.error.item.unauthorized', args: ['Read System Info', 'Rundeck', ""]])
        }
        Date nowDate=new Date();
        String nodeName= servletContext.getAttribute("FRAMEWORK_NODE")
        String appVersion= grailsApplication.metadata['app.version']
        double load= ManagementFactory.getOperatingSystemMXBean().systemLoadAverage
        int processorsCount= ManagementFactory.getOperatingSystemMXBean().availableProcessors
        String osName= ManagementFactory.getOperatingSystemMXBean().name
        String osVersion= ManagementFactory.getOperatingSystemMXBean().version
        String osArch= ManagementFactory.getOperatingSystemMXBean().arch
        String javaVendor=System.properties['java.vendor']
        String javaVersion=System.properties['java.version']
        String vmName=ManagementFactory.getRuntimeMXBean().vmName
        String vmVendor=ManagementFactory.getRuntimeMXBean().vmVendor
        String vmVersion=ManagementFactory.getRuntimeMXBean().vmVersion
        long durationTime=ManagementFactory.getRuntimeMXBean().uptime
        Date startupDate = new Date(nowDate.getTime()-durationTime)
        int threadActiveCount=Thread.activeCount()
        def metricsJsonUrl = createLink(uri: '/metrics/metrics?pretty=true',absolute: true)
        def metricsThreadDumpUrl = createLink(uri: '/metrics/threads',absolute: true)
        return apiService.renderSuccessXml(request,response){
            if(apiService.doWrapXmlResponse(request)){
                delegate.'success' {
                    delegate.'message'("System Stats for Rundeck ${appVersion} on node ${nodeName}")
                }
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
                    vendor(javaVendor)
                    version(javaVersion)
                    implementationVersion(vmVersion)
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
                metrics(href:metricsJsonUrl,contentType:'text/json')
                threadDump(href:metricsThreadDumpUrl,contentType:'text/plain')
            }
        }
    }
}
