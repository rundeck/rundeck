package org.rundeck.app.data.options

import com.dtolabs.rundeck.core.options.RemoteJsonOptionRetriever
import groovy.util.logging.Log4j2
import org.grails.web.json.JSONObject
import org.rundeck.app.data.model.v1.job.JobData
import org.rundeck.app.data.model.v1.job.option.OptionData
import org.rundeck.app.job.option.JobOptionUrlExpander
import org.rundeck.app.job.option.RemoteOptionValueLoader
import org.rundeck.app.job.option.RemoteOptionValuesResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import rundeck.data.util.JobDataUtil
import rundeck.services.ConfigurationService
import rundeck.services.FrameworkService

@Log4j2
class DefaultRemoteOptionValueLoader implements RemoteOptionValueLoader {

    public final String REMOTE_OPTION_DISABLE_JSON_CHECK = 'project.jobs.disableRemoteOptionJsonCheck'

    RemoteJsonOptionRetriever remoteJsonOptionRetriever
    JobOptionUrlExpander jobOptionUrlExpander
    ConfigurationService configurationService
    FrameworkService frameworkService
    @Override
    RemoteOptionValuesResponse loadOptionsRemoteValues(JobData jobData, Map mapConfig, String username) {
        //load expand variables in URL source
        OptionData opt = jobData.optionSet.find { it.name == mapConfig.option }
        def realUrl = opt.realValuesUrl.toExternalForm()
        String srcUrl = jobOptionUrlExpander.expandUrl(realUrl, jobData, opt, mapConfig.extra?.option)
        String cleanUrl = srcUrl.replaceAll("^(https?://)([^:@/]+):[^@/]*@", '$1$2:****@');
        def remoteResult = [:]
        def result = null
        def remoteStats = [startTime: System.currentTimeMillis(), httpStatusCode: "", httpStatusText: "", contentLength: "", url: srcUrl, durationTime: "", finishTime: "", lastModifiedDateTime: ""]
        def err = [:]
        int timeout = 10
        int contimeout = 0
        int retryCount = 5
        int httpResponseCode = 0

        if (configurationService.getString("jobs.options.remoteUrlTimeout")) {
            try {
                timeout = configurationService.getInteger("jobs.options.remoteUrlTimeout", null)
            } catch (NumberFormatException e) {
                log.warn(
                        "Configuration value rundeck.jobs.options.remoteUrlTimeout is not a valid integer: "
                                + e.message
                )
            }
        }
        if (configurationService.getString("jobs.options.remoteUrlConnectionTimeout")) {
            try {
                contimeout = configurationService.getInteger("jobs.options.remoteUrlConnectionTimeout", null)
            } catch (NumberFormatException e) {
                log.warn(
                        "Configuration value rundeck.jobs.options.remoteUrlConnectionTimeout is not a valid integer: "
                                + e.message
                )
            }
        }
        if (configurationService.getString("jobs.options.remoteUrlRetry")) {
            try {
                retryCount = configurationService.getInteger("jobs.options.remoteUrlRetry", null)
            } catch (NumberFormatException e) {
                log.warn(
                        "Configuration value rundeck.jobs.options.remoteUrlRetry is not a valid integer: "
                                + e.message
                )
            }
        }
        if (srcUrl.indexOf('#') >= 0 && srcUrl.indexOf('#') < srcUrl.size() - 1) {
            def urlanchor = new HashMap<String, String>()
            def anchor = srcUrl.substring(srcUrl.indexOf('#') + 1)
            def parts = anchor.split(";")
            parts.each { s ->
                def subpart = s.split("=", 2)
                if (subpart && subpart.length == 2 && subpart[0] && subpart[1]) {
                    urlanchor[subpart[0]] = subpart[1]
                }
            }
            if (urlanchor['timeout']) {
                try {
                    timeout = Integer.parseInt(urlanchor['timeout'])
                } catch (NumberFormatException e) {
                    log.warn(
                            "URL timeout ${urlanchor['timeout']} is not a valid integer: "
                                    + e.message
                    )
                }
            }
            if (urlanchor['contimeout']) {
                try {
                    contimeout = Integer.parseInt(urlanchor['contimeout'])
                } catch (NumberFormatException e) {
                    log.warn(
                            "URL contimeout ${urlanchor['contimeout']} is not a valid integer: "
                                    + e.message
                    )
                }
            }
            if (urlanchor['retry']) {
                try {
                    retryCount = Integer.parseInt(urlanchor['retry'])
                } catch (NumberFormatException e) {
                    log.warn(
                            "URL retry ${urlanchor['retry']} is not a valid integer: "
                                    + e.message
                    )
                }
            }
        }

        int count = retryCount

        //cycle to retry if getRemoteJSON dont get the remote values
        do{
            try {
                //validate if not the firt attemp
                if(retryCount > count){
                    Thread.sleep(contimeout*1000)
                }
                def framework = frameworkService.getRundeckFramework()
                def projectConfig = framework.frameworkProjectMgr.loadProjectConfig(jobData.project)
                boolean disableRemoteOptionJsonCheck = projectConfig.hasProperty(REMOTE_OPTION_DISABLE_JSON_CHECK)

                remoteResult = remoteJsonOptionRetriever.getRemoteJson(srcUrl, timeout, contimeout, retryCount, disableRemoteOptionJsonCheck)
                result = remoteResult.json
                if (remoteResult.stats) {
                    remoteStats.putAll(remoteResult.stats)
                    if(remoteResult.stats.httpStatusCode){
                        httpResponseCode = remoteResult.stats.httpStatusCode
                    }
                }
            } catch (Exception e) {
                err.message = "Failed loading remote option values"
                err.exception = e
                err.srcUrl = cleanUrl
                log.error("getRemoteJSON error: URL ${cleanUrl} : ${e.message}");
                e.printStackTrace()
                remoteStats.finishTime = System.currentTimeMillis()
                remoteStats.durationTime = remoteStats.finishTime - remoteStats.startTime
            }
            if (remoteResult.error) {
                err.message = "Failed loading remote option values"
                err.exception = new Exception(remoteResult.error)
                err.srcUrl = cleanUrl
                log.error("getRemoteJSON error: URL ${cleanUrl} : ${remoteResult.error}");
            }
            logRemoteOptionStats(remoteStats, [jobName: JobDataUtil.generateFullName(jobData), id: JobDataUtil.getExtId(jobData), jobProject: jobData.project, optionName: mapConfig.option, user: username])
            count--
        }while(count > 0 && (httpResponseCode < 200 || httpResponseCode > 300 ))

        //validate result contents
        boolean valid = true;
        def validationerrors = []
        if (result) {
            if (result instanceof Collection) {
                def resultForString = []
                result.eachWithIndex { entry, i ->
                    if (entry instanceof JSONObject) {
                        if (!entry.name) {
                            validationerrors << "Item: ${i} has no 'name' entry"
                            valid = false;
                        }
                        if (!entry.value) {
                            validationerrors << "Item: ${i} has no 'value' entry"
                            valid = false;
                        }
                    } else if (!(entry instanceof String)) {
                        valid = false;
                        validationerrors << "Item: ${i} expected string or map like {name:\"..\",value:\"..\"}"
                    } else if (entry instanceof String){
                        resultForString << [name: entry, value: entry]
                    }
                }
                if(!resultForString.isEmpty()){
                    result = resultForString
                }
            } else if (result instanceof JSONObject) {
                JSONObject jobject = result
                result = []
                jobject.keys().each { k ->
                    result << [name: k, value: jobject.get(k)]
                }
            } else {
                validationerrors << "Expected top-level list with format: [{name:\"..\",value:\"..\"},..], or ['value','value2',..] or simple object with {name:\"value\",...}"
                valid = false
            }
            if (!valid) {
                result = null
                err.message = "Failed parsing remote option values: ${validationerrors.join('\n')}"
                err.code = 'invalid'
            }
            result = sortRemoteOptions(result, opt.sortValues?opt.sortValues:false)
        } else if (!err) {
            err.message = "Empty result"
            err.code = 'empty'
        }
        return new RemoteOptionValuesResponse(
                optionSelect : opt,
                values       : result,
                srcUrl       : cleanUrl,
                err          : err
        )
    }

    /**
     * It sorts the url options based on the option label
     * @param List optionValues
     * @param boolean sortValues
     * @return List
     */
    def sortRemoteOptions(List<JSONObject> optionValues, boolean sortValues){
        if(optionValues && sortValues){
            Collections.sort(optionValues, new Comparator<Map<String, String>>() {
                public int compare(final Map<String, String> o1, final Map<String, String> o2) {
                    return o1.get("name").compareTo(o2.get("name"))
                }
            })
        }
        return optionValues
    }

    static Logger optionsLogger = LoggerFactory.getLogger("com.dtolabs.rundeck.remoteservice.http.options")
    private logRemoteOptionStats(stats,jobdata){
        stats.keySet().each{k->
            def v= stats[k]
            if(v instanceof Date){
                //TODO: reformat date
                MDC.put(k,v.toString())
                MDC.put("${k}Time",v.time.toString())
            }else if(v instanceof String){
                MDC.put(k,v?v:"-")
            }else{
                final string = v.toString()
                MDC.put(k, string?string:"-")
            }
        }
        jobdata.keySet().each{k->
            final var = jobdata[k]
            MDC.put(k,var?var.toString():'-')
        }
        optionsLogger.info(stats.httpStatusCode + " " + stats.httpStatusText+" "+stats.contentLength+" "+stats.url)
        stats.keySet().each {k ->
            if (stats[k] instanceof Date) {
                //reformat date
                MDC.remove(k+'Time')
            }
            MDC.remove(k)
        }
        jobdata.keySet().each {k ->
            MDC.remove(k)
        }
    }
}
