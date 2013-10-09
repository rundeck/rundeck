package rundeck.services

import groovy.xml.MarkupBuilder
import rundeck.Execution
import rundeck.filters.ApiRequestFilters

import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat

class ApiService {
    static transactional = false
    def messageSource

    def respondOutput(HttpServletResponse response, String contentType, String output) {
        response.setContentType(contentType)
        response.setCharacterEncoding('UTF-8')
        def out = response.outputStream
        out << output
        out.flush()
        null
    }
    def respondXml(HttpServletResponse response, Closure recall) {
        return respondOutput(response, 'text/xml', renderXml(recall))
    }

    def renderXml(Closure recall) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.with {
            recall.delegate = delegate
            recall()
        }
        return writer.toString()
    }

    def renderSuccessXml(HttpServletResponse response, String code, List args) {
        return renderSuccessXml(response){
            success{
                message(messageSource.getMessage(code,args as Object[],null))
            }
        }
    }
    def renderSuccessXml(HttpServletResponse response, Closure recall) {
        return respondOutput(response, 'text/xml', renderSuccessXml(recall))
    }
    def renderSuccessXml(Closure recall){
        return renderXml {
            result(success: "true", apiversion: ApiRequestFilters.API_CURRENT_VERSION) {
//                recall.resolveStrategy=Closure.DELEGATE_FIRST
                recall.delegate = delegate
                recall()
            }
        }
    }

    def renderErrorXml(HttpServletResponse response, Map error){
        if(error.status){
            response.setStatus(error.status)
        }
        return respondOutput(response, 'text/xml', renderErrorXml(error, error.code))
    }
    /**
     * Require all specified parameters in the request
     * @param request
     * @param response
     * @param params list of parameters of which all must be present
     * @return false if requirement is not met, response will already have been made
     */
    def requireParameters(Map reqparams,HttpServletResponse response,List<String> params){
        def notfound=params.find{!reqparams[it]}
        if(notfound){
            renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: [notfound]])
            return false
        }
        return true
    }
    /**
     * Require any of the specified parameters in the request
     * @param request
     * @param response
     * @param params list of parameters of which one must be present
     * @return false if requirement is not met, response will already have been made
     */
    def requireAnyParameters(Map reqparams,HttpServletResponse response,List<String> params){
        def found=params.any{ reqparams[it]}
        if(!found){
            renderErrorXml(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['Any of: '+params.join(', ')]])
            return false
        }
        return true
    }
    /**
     * Require a value exists, or respond with NOT FOUND
     * @param request
     * @param response
     * @param item
     * @return false if requirement is not met, response will already have been made
     */
    def requireExists(HttpServletResponse response, Object item, List args) {
        if (!item) {
            renderErrorXml(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: args])
            return false
        }
        return true
    }
    /**
     * Require a minimum API version, and optional maximum
     * @param request
     * @param response
     * @param min
     * @param max
     * @return false if requirement is not met: response will already have been made
     */
    def requireVersion(request, HttpServletResponse response, int min, int max = 0){
        if (request.api_version < min) {
            renderErrorXml(response,[
                    status:HttpServletResponse.SC_BAD_REQUEST,
                    code:'api.error.api-version.unsupported',
                    args: [request.api_version, request.forwardURI, "Minimum supported version: " + min]
            ])
            return false
        }
        if (max > 0 && request.api_version > max) {
            renderErrorXml(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.api-version.unsupported',
                    args: [request.api_version, request.forwardURI, "Maximum supported version: " + max]
            ])
            return false
        }
        return true
    }
    def renderErrorXml(messages, String code=null, builder=null){
        def writer = new StringWriter()
        def xml
        if(!builder){
            xml = new MarkupBuilder(writer)
        }else{
            xml=builder
        }
        xml.with {
            result(error: "true", apiversion: ApiRequestFilters.API_CURRENT_VERSION) {
                def errorprops = [:]
                if (code) {
                    errorprops = [code: code]
                }
                delegate.'error'(errorprops) {
                    if (!messages) {
                        delegate.'message'(messageSource.getMessage("api.error.unknown",null,null))
                    }
                    if (messages instanceof List) {
                        messages.each {
                            delegate.'message'(it)
                        }
                    }else if(messages instanceof Map && messages.code){
                        delegate.'message'(messages.message?:messageSource.getMessage(messages.code, messages.args?messages.args as Object[]:null, null))
                    }
                }
            }
        }
        if(!builder){
            return writer.toString()
        }
    }

    /**
     * Render execution document for api response
     */

    public def respondExecutionsXml(HttpServletResponse response,execlist,paging=[:]) {
        return respondOutput(response, 'text/xml', renderSuccessXml{
            renderExecutionsXml(execlist, paging, delegate)
        })
    }
    /**
     * Render execution list xml given a List of executions, and a builder delegate
     * @param execlist list of Maps containing [execution:Execution, href: URL to execution, status: rendered status text, summary: rendered summary text]
     */
    public def renderExecutionsXml(execlist, paging = [:], delegate){
        def execAttrs = [count: execlist.size()]
        if (paging) {
            execAttrs.putAll(paging)
        }
        delegate.'executions'(execAttrs) {
            execlist.each { Map execdata ->

                def href=execdata.href
                def status=execdata.status
                def summary=execdata.summary
                def Execution e = Execution.get(execdata.execution.id)
                execution(
                        /** attributes   **/
                        id: e.id,
                        href: href,
                        status: status,
                        project: e.project
                ) {
                    /** elements   */
                    user(e.user)
                    delegate.'date-started'(unixtime: e.dateStarted.time, w3cDateValue(e.dateStarted))
                    if (null != e.dateCompleted) {
                        delegate.'date-ended'(unixtime: e.dateCompleted.time, w3cDateValue(e.dateCompleted))
                    }
                    if (e.cancelled) {
                        abortedby(e.abortedby ? e.abortedby : e.user)
                    }
                    if (e.scheduledExecution) {
                        def jobparams = [id: e.scheduledExecution.extid]
                        if (e.scheduledExecution.totalTime >= 0 && e.scheduledExecution.execCount > 0) {
                            def long avg = Math.floor(e.scheduledExecution.totalTime / e.scheduledExecution.execCount)
                            jobparams.averageDuration = avg
                        }
                        job(jobparams) {
                            name(e.scheduledExecution.jobName)
                            group(e.scheduledExecution.groupPath ?: '')
                            project(e.scheduledExecution.project)
                            description(e.scheduledExecution.description)
                        }
                    }
                    description(summary)
                    argstring(e.argString)
                }
            }
        }
    }

    def w3cDateValue(Date date) {
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormater.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormater.format(date);
    }
}
