package rundeck.services

import groovy.xml.MarkupBuilder
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.filters.ApiRequestFilters

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat

class ApiService {
    static transactional = false
    def messageSource
    def grailsLinkGenerator

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
    def renderSuccessXml(int status=0,HttpServletResponse response, Closure recall) {
        if(status){
            response.status=status
        }
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

    /**
     * Return the final portion of the request URI with the stripped extension restored
     * @param request request
     * @param paramValue value of final path parameter extracted via URL mapping, e.g. "/path/$paramValue**"
     * @return paramValue with stripped file extension restored, or paramValue if it had no file extension
     */
    public String restoreUriPath(HttpServletRequest request, String paramValue){
        def lastpath= request.forwardURI.substring(request.forwardURI.lastIndexOf('/')+1)
        def extension= lastpath.indexOf('.')>=0?lastpath.substring(lastpath.lastIndexOf('.')+1):null
        if(extension && request.forwardURI.endsWith(paramValue+'.'+extension)){
            return paramValue+'.'+extension
        }
        return paramValue
    }
    /**
     * Determine appropriate response format based on allowed formats or request format. If the requested response
     * type is in the allowed formats it is returned, otherwise if a default format is specified it is used.  If the
     * response format is not in the allowed formats and no default is specified, the request content type format is
     * returned.
     * @param request request
     * @param response response
     * @param allowed list of allowed formats
     * @param defformat default format, or null to use the request format
     * @return format name
     */
    public String extractResponseFormat(HttpServletRequest request, HttpServletResponse response,
                                      ArrayList<String> allowed, String defformat = null) {
        return ((response.format in allowed) ? response.format : (defformat ?: request.format))
    }
    /**
     * Require request to be a certain format, returns false if not valid and error response is already sent
     * @param request request
     * @param response response
     * @param allowed allowed formats
     * @param responseFormat response format to send ('xml' or 'json') if request is not valid, or null to use default
     * @return true if valid, false otherwise
     */
    def requireRequestFormat(HttpServletRequest request, HttpServletResponse response, ArrayList<String> allowed, def
    responseFormat = null) {
        def test = request.format in allowed
        if (!test) {
            //bad request
            renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: "api.error.invalid.request",
                            args: ["Expected request content to be one of allowed formats: [" + allowed.join(', ' +
                                    '') + "], " +
                                    "but was: " +
                                    "${request.getHeader('Content-Type')}"],
                            format: responseFormat
                    ])
        }
        test
    }

    /**
     * Parse XML or JSON input formatted data, and handle with appropriate closure.  If the input format is not
     * supported, or there is an error parsing the input, an error response is sent, and false is returned.
     * @param request request
     * @param response response
     * @param handlers handler map, using keys 'xml' or 'json'.
     * @return true if parsing was successful, false if an error occurred and a response has already been sent
     */
    public boolean parseJsonXmlWith(HttpServletRequest request, HttpServletResponse response,
                                    Map<String, Closure> handlers) {
        def respFormat = extractResponseFormat(request, response, ['xml', 'json'])
        if (!requireRequestFormat(request, response, ['xml', 'json'], respFormat)) {
            return false
        }
        String error
        request.withFormat {
            json {
                if (handlers.json) {
                    try {
                        def parsed = request.JSON

                        if (!parsed) {
                            error = "Could not parse JSON"
                        } else {
                            handlers.json(parsed)
                        }
                    } catch (ConverterException e) {
                        error = e.message + (e.cause ? ": ${e.cause.message}" : '')
                    }
                }else{
                    error="Unexpected content type: ${request.getHeader('Content-Type')}"
                }
            }
            xml {
                if (handlers.xml) {
                    try {
                        handlers.xml(request.XML)
                    } catch (ConverterException e) {
                        error = e.message + (e.cause ? ": ${e.cause.message}" : '')
                    }
                } else {
                    error = "Unexpected content type: ${request.getHeader('Content-Type')}"
                }
            }
        }
        if (error) {
            renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    message: error,
                    format: respFormat
            ])
            return false
        }
        return true
    }

    /**
     * Render error in either JSON or XML format, depending on expected response
     * @param response
     * @param error
     * @return
     */
    def renderErrorFormat(HttpServletResponse response, Map error){
        def resp=[xml: this.&renderErrorXml,json: this.&renderErrorJson, text:{resp,err->
            response.outputStream<< renderErrorText(err)
        }]
        def respFormat = error.format && resp[error.format] ?
            error.format :
            response.format && resp[response.format] ?
                response.format :
                'xml'
        return resp[respFormat](response,error)
    }
    def renderErrorXml(HttpServletResponse response, Map error){
        if(error.status){
            response.setStatus(error.status)
        }
        return respondOutput(response, 'text/xml', renderErrorXml(error, error.code))
    }
    def renderErrorJson(HttpServletResponse response, Map error){
        if(error.status){
            response.setStatus(error.status)
        }
        return respondOutput(response, 'application/json', renderErrorJson(error, error.code))
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

    def renderErrorText(messages, String code=null){
        if (!messages) {
            return messageSource.getMessage("api.error.unknown", null, null)
        }
        if (messages instanceof List) {
            return messages.join("\r\n")
        }else if (messages instanceof Map && messages.message) {
            return messages.message
        } else if (messages instanceof Map && messages.code) {
            return messageSource.getMessage(messages.code, messages.args ? messages.args as Object[] : null, null)
        }
        return messages.toString()
    }
    def renderErrorJson(messages, String code=null){
        def result=[
                error: true,
                apiversion: ApiRequestFilters.API_CURRENT_VERSION,
        ]
        if (code) {
            result.errorCode=code
        }
        if (!messages) {
            result.'message'=messageSource.getMessage("api.error.unknown", null, null)
        }
        if (messages instanceof List) {
            result.messages=messages
        } else if (messages instanceof Map && messages.code) {
            result.message=(messages.message ?: messageSource.getMessage(messages.code, messages.args ? messages.args as Object[] : null, null))
        }else if (messages instanceof Map && messages.message) {
            result.message=messages.message
        }
        return result.encodeAsJSON()
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
                    }else if(messages instanceof Map && messages.message){
                        delegate.'message'(messages.message)
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
                            if(e.argString){
                                options{
                                    FrameworkService.parseOptsFromString(e.argString).each{k,v->
                                        option(name:k,value:v)
                                    }
                                }
                            }
                        }
                    }
                    description(summary)
                    argstring(e.argString)
                    if(e.succeededNodeList){
                        successfulNodes{
                            e.succeededNodeList.split(',').each {
                                node(name:it)
                            }
                        }
                    }
                    if(e.failedNodeList){
                        failedNodes {
                            e.failedNodeList.split(',').each {
                                node(name: it)
                            }
                        }
                    }
                }
            }
        }
    }

    def w3cDateValue(Date date) {
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormater.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormater.format(date);
    }

    String apiHrefForJob(ScheduledExecution scheduledExecution) {
        return grailsLinkGenerator.link(controller: 'scheduledExecution',
                id: scheduledExecution.extid,
                params: [api_version:ApiRequestFilters.API_CURRENT_VERSION],
                absolute: true)
    }
    String apiHrefForExecution(Execution execution) {
        return grailsLinkGenerator.link(controller: 'execution', id: execution.id,
                params: [api_version: ApiRequestFilters.API_CURRENT_VERSION],
                absolute: true)
    }
}
