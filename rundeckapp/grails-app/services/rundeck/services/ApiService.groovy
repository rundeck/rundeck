package rundeck.services

import com.dtolabs.rundeck.core.authorization.Validation
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.converters.JSON
import grails.web.JSONBuilder
import groovy.xml.MarkupBuilder
import org.apache.commons.lang.RandomStringUtils
import org.codehaus.groovy.grails.web.converters.exceptions.ConverterException
import rundeck.AuthToken
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.User
import rundeck.filters.ApiRequestFilters

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat

class ApiService {
    static transactional = false
    public static final String TEXT_XML_CONTENT_TYPE = 'text/xml'
    public static final String APPLICATION_XML_CONTENT_TYPE = 'application/xml'
    public static final String JSON_CONTENT_TYPE = 'application/json'
    public static final String XML_API_RESPONSE_WRAPPER_HEADER = "X-Rundeck-API-XML-Response-Wrapper"
    def messageSource
    def grailsLinkGenerator

    public static final Map<String,String> HTTP_METHOD_ACTIONS = Collections.unmodifiableMap (
            POST: AuthConstants.ACTION_CREATE,
            PUT: AuthConstants.ACTION_UPDATE,
            GET: AuthConstants.ACTION_READ,
            DELETE: AuthConstants.ACTION_DELETE
    )
    private String genRandomString() {
        return RandomStringUtils.random(32, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
    }
    /**
     * Generate a new unique auth token for the user and return it
     * @param u
     * @return
     */
    AuthToken generateAuthToken(User u){
        String newtoken = genRandomString()
        while (AuthToken.findByToken(newtoken) != null) {
            newtoken = genRandomString()
        }
        AuthToken token = new AuthToken(token: newtoken, authRoles: 'api_token_group', user: u)

        if (token.save()) {
            log.debug("GENERATE TOKEN ${newtoken} for User ${u.login} with roles: ${token.authRoles}")
            return token
        } else {
            throw new Exception("Failed to save token for User ${u.login}")
        }
    }
    def respondOutput(HttpServletResponse response, String contentType, String output) {
        response.setContentType(contentType)
        response.setCharacterEncoding('UTF-8')
        response.setHeader("X-Rundeck-API-Version",ApiRequestFilters.API_CURRENT_VERSION.toString())
        def out = response.outputStream
        out << output
        out.flush()
        null
    }
    def respondXml(HttpServletResponse response, Closure recall) {
        return respondOutput(response, TEXT_XML_CONTENT_TYPE, renderXml(recall))
    }

    def renderXml(Closure recall) {
        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.with {
            recall.delegate = delegate
            recall.resolveStrategy=Closure.DELEGATE_FIRST
            recall()
        }
        return writer.toString()
    }

    /**
     * Render xml response
     * @param request
     * @param response
     * @param code
     * @param args
     */
    def renderSuccessXml(HttpServletRequest request,HttpServletResponse response, String code, List args) {
        return renderSuccessXmlWrap(request,response) {
            success {
                message(messageSource.getMessage(code, args as Object[], null))
            }
        }
    }

    /**
     * Return true if the request indicates the XML response content should be wrapped in a '&lt;result&gt;'
     * element.  Return true if:
     * <ul>
     * <li>less-than: "11", and {@value  #XML_API_RESPONSE_WRAPPER_HEADER} header is not "false"</li>
     * <li>OR, greater-than: "10" AND {@value  #XML_API_RESPONSE_WRAPPER_HEADER} header is "true"</li>
     * </ul>
     * @param request
     * @return
     */
    public boolean doWrapXmlResponse(HttpServletRequest request) {
        if(request.api_version < ApiRequestFilters.V11){
            //require false to disable wrapper
            return !"false".equals(request.getHeader(XML_API_RESPONSE_WRAPPER_HEADER))
        } else{
            //require true to enable wrapper
            return "true".equals(request.getHeader(XML_API_RESPONSE_WRAPPER_HEADER))
        }
    }

    def renderSuccessXml(HttpServletResponse response, String code, List args) {
        return renderSuccessXml(null,response,code,args)
    }
    /**
     * Render xml response, forces "&lt;result&gt;" wrapper
     * @param status status code to send
     * @param request
     * @param response
     * @param recall
     * @return
     */
    def renderSuccessXmlWrap(HttpServletRequest request,
                         HttpServletResponse response, Closure recall) {
        return renderSuccessXml(0,true,request,response,recall)
    }
    /**
     * Render xml response, provides "&lt;result&gt;" wrapper for api request older than v11,
     * or if "X-Rundeck-api-xml-response-wrapper" header in request is "true".
     * @param status status code to send
     * @param request
     * @param response
     * @param recall
     * @return
     */
    def renderSuccessXml(int status = 0, Boolean forceWrapper = false, HttpServletRequest request,
                             HttpServletResponse response, Closure recall) {
        if (status) {
            response.status = status
        }
        if (!request || doWrapXmlResponse(request) || forceWrapper) {
            response.setHeader(XML_API_RESPONSE_WRAPPER_HEADER,"true")
            return respondOutput(response, TEXT_XML_CONTENT_TYPE, renderSuccessXml(recall))
        }else{
            response.setHeader(XML_API_RESPONSE_WRAPPER_HEADER, "false")
            return respondOutput(response, APPLICATION_XML_CONTENT_TYPE, renderSuccessXmlUnwrapped(recall))
        }
    }
    /**
     *
     * @param status
     * @param response
     * @param recall
     * @return
     * @deprecated use {@link #renderSuccessXml(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, groovy.lang.Closure)}
     */
    def renderSuccessXml(int status=0,HttpServletResponse response, Closure recall) {
       return renderSuccessXml (status,false,null,response,recall)
    }
    def renderSuccessXmlUnwrapped(Closure recall){
        return renderXml(recall)
    }
    def renderSuccessXml(Closure recall){
        return renderSuccessXmlUnwrapped {
            result(success: "true", apiversion: ApiRequestFilters.API_CURRENT_VERSION) {
                recall.delegate = delegate
                recall.resolveStrategy=Closure.DELEGATE_FIRST
                recall()
            }
        }
    }
    /**
     * Render JSON to the response, using a builder with the closure
     * @param response
     * @param recall
     */
    def renderSuccessJson(HttpServletResponse response,Closure recall){
        response.contentType=JSON_CONTENT_TYPE
        response.characterEncoding='UTF-8'
        JSONBuilder builder = new JSONBuilder();
        JSON json = builder.build(recall);
        json.render(response);
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
     * @param allowed allowed formats or mime-types
     * @param responseFormat response format to send ('xml' or 'json') if request is not valid, or null to use default
     * @return true if valid, false otherwise
     */
    def requireRequestFormat(HttpServletRequest request, HttpServletResponse response, ArrayList<String> allowed, def
    responseFormat = null) {
        def contentType = request.getHeader("Content-Type")
        def test = request.format in allowed || (contentType && (extractMimeType(contentType) in allowed))
        if (!test) {
            //bad request
            renderErrorFormat(response,
                    [
                            status: HttpServletResponse.SC_BAD_REQUEST,
                            code: "api.error.invalid.request",
                            args: ["Expected request content to be one of allowed formats: [" + allowed.join(', ' +
                                    '') + "], " +
                                    "but was: " +
                                    "${contentType}"],
                            format: responseFormat
                    ])
        }
        test
    }

    String extractMimeType(String s) {
        return s.contains(';') ? s.split(';')[0].trim(): s;
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
            if (err.status) {
                response.setStatus(err.status)
            }
            response.outputStream<< renderErrorText(err)
        }]
        def eformat = error.format
        def rformat = response.format
        def respFormat = eformat && resp[eformat] ? eformat :
            rformat && resp[rformat] ? rformat : 'xml'
        return resp[respFormat](response,error)
    }
    def renderErrorXml(HttpServletResponse response, Map error){
        if(error.status){
            response.setStatus(error.status)
        }
        return respondOutput(response, TEXT_XML_CONTENT_TYPE, renderErrorXml(error, error.code))
    }
    def renderErrorJson(HttpServletResponse response, Map error){
        if(error.status){
            response.setStatus(error.status)
        }
        return respondOutput(response, JSON_CONTENT_TYPE, renderErrorJson(error, error.code))
    }
    /**
     * Require all specified parameters in the request, send json/xml response based on accept header
     * @param request
     * @param response
     * @param params list of parameters of which all must be present
     * @return false if requirement is not met, response will already have been made
     */
    def requireParametersFormat(Map reqparams,HttpServletResponse response,List<String> params){
        def notfound=params.find{!reqparams[it]}
        if(notfound){
            renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: [notfound]])
            return false
        }
        return true
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
            renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
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
            renderErrorFormat(response, [status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.parameter.required', args: ['Any of: '+params.join(', ')]])
            return false
        }
        return true
    }
    /**
     * Require a value exists, or respond with NOT FOUND, and format response as xml/json based on accept header
     * @param request
     * @param response
     * @param item
     * @param args arguments to error message: {@literal '{0} does not exist: {1}'}
     * @return false if requirement is not met, response will already have been made
     */
    def requireExistsFormat(HttpServletResponse response, Object item, List args) {
        if (!item) {
            renderErrorFormat(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: 'api.error.item.doesnotexist', args: args])
            return false
        }
        return true
    }
    /**
     * Require a value exists, or respond with NOT FOUND
     * @param request
     * @param response
     * @param item
     * @param args arguments to error message: {@literal '{0} does not exist: {1}'}
     * @return false if requirement is not met, response will already have been made
     */
    def requireExists(HttpServletResponse response, Object item, List args, String code=null) {
        if (!item) {
            renderErrorFormat(response, [status: HttpServletResponse.SC_NOT_FOUND,
                    code: code?:'api.error.item.doesnotexist', args: args])
            return false
        }
        return true
    }
    /**
     * Require that the original request is via the /api URL mapping
     * @param request
     * @param response
     * @return false if it is not a valid API request. If false, then an error status response has already been sent
     */
    def requireApi(request, HttpServletResponse response){
        if(!request.api_version){
            //not a /api URL
            response.sendError(HttpServletResponse.SC_NOT_FOUND)

            return false
        }
        true
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
        if(!requireApi(request,response)){
            return false
        }
        if (request.api_version < min) {
            renderErrorFormat(response,[
                    status:HttpServletResponse.SC_BAD_REQUEST,
                    code:'api.error.api-version.unsupported',
                    args: [request.api_version, request.forwardURI, "Minimum supported version: " + min]
            ])
            return false
        }
        if (max > 0 && request.api_version > max) {
            renderErrorFormat(response, [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code: 'api.error.api-version.unsupported',
                    args: [request.api_version, request.forwardURI, "Maximum supported version: " + max]
            ])
            return false
        }
        return true
    }

    def requireAuthorized(authorized, HttpServletResponse response, Object[] args = []) {
        if (!authorized) {
            renderErrorFormat(response,
                              [
                                      status: HttpServletResponse.SC_FORBIDDEN,
                                      code  : "api.error.item.unauthorized",
                                      args  : args
                              ]
            )
        }
        return authorized
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
        return result.encodeAsJSON().toString()
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
     * in Json or XML, render a file as a wrapped strings specified by a 'contents' entry/element
     * @param contentString
     * @param request
     * @param response
     * @param respFormat
     * @param delegate
     * @return
     */
    void renderWrappedFileContents(
            String contentString,
            String respFormat,
            delegate
    )
    {
        if (respFormat=='json') {
            delegate.contents = contentString
        }else{
            delegate.'contents' {
                mkp.yieldUnescaped("<![CDATA[" + contentString.replaceAll(']]>', ']]]]><![CDATA[>') + "]]>")
            }
        }
    }

    /**
     * Render json response for dir listing
     * @param path project file path
     * @param genpath closure called with path string to export the path
     * @param genhref closure called with path string to generate href
     * @param builder builder
     * @return
     */
     void jsonRenderDirlist(String path,Closure genpath,Closure genhref,List<String>dirlist,builder){
        builder.with{
            delegate.'path'=genpath(path)
            delegate.'type'='directory'
            //delegate.'name'= pathName(path)
            delegate.'href'= genhref(path)
            delegate.'resources'=array{
                def builder2=delegate
                dirlist.each{dirpath->
                    builder2.element {
                        delegate.'path'=genpath(dirpath)
                        delegate.'type'=dirpath.endsWith('/')?'directory':'file'
                        if(!dirpath.endsWith('/')) {
                            delegate.'name' = pathName(genpath(dirpath))
                        }
                        delegate.'href'= genhref(dirpath)
                    }
                }
            }
        }
    }
    /**
     * Render xml response for dir listing
     * @param path project file path
     * @param genpath closure called with path string to export the path
     * @param genhref closure called with path string to generate href
     * @param builder builder
     * @return
     */
     void xmlRenderDirList(String path,Closure genpath,Closure genhref,List<String>dirlist,builder){
        builder.'resource'(
                path:genpath(path),
                type:'directory',
                href:genhref(path)) {

            delegate.'contents' {
                def builder2 = delegate
                dirlist.each { dirpath ->
                    def resmap = [
                            path: genpath(dirpath),
                            type: dirpath.endsWith('/') ? 'directory' : 'file',
                            href: genhref(dirpath)
                    ]
                    if (!dirpath.endsWith('/')) {
                        resmap.'name' = pathName(genpath(dirpath))
                    }
                    builder2.'resource'(resmap)
                }
            }
        }
    }
    String pathRmPrefix(String path,String prefix) {
        prefix&&path.startsWith(prefix)?path.substring(prefix.length()):path
    }

    String pathName(String path) {
        path.lastIndexOf('/')>=0?path.substring(path.lastIndexOf('/') + 1):path
    }

    public void renderJsonAclpolicyValidation(Validation validation, builder){
        builder.valid = validation.valid
        if(!validation.valid) {
            builder.'policies' = builder.array {
                def d=delegate
                validation.errors.keySet().sort().each { ident ->
                    builder.'element'(policy: ident, errors: validation.errors[ident])
                }
            }
        }
    }
    public void renderXmlAclpolicyValidation(Validation validation, builder){
        builder.'validation'(valid:validation.valid){
            validation.errors?.keySet().sort().each{ident->
                policy(id:ident){
                    validation.errors[ident].each{
                        delegate.error(it)
                    }
                }
            }
        }
    }
    /**
     * Render execution document for api response
     */
    public def respondExecutionsXml(HttpServletRequest request,HttpServletResponse response,execlist,paging=[:]) {
        renderSuccessXml(request,response){
            renderExecutionsXml(execlist, paging, delegate)
        }
    }
    /**
     * Render execution document for api response
     */
    public def respondExecutionsJson(HttpServletRequest request,HttpServletResponse response,execlist,paging=[:]) {
        renderSuccessJson(response){
            renderExecutionsJson(execlist, paging, delegate)
        }
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
                        permalink: execdata.permalink,
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
                        jobparams.'href'=(apiHrefForJob(e.scheduledExecution))
                        jobparams.'permalink'=(guiHrefForJob(e.scheduledExecution))
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
                    if(e.serverNodeUUID){
                        serverUUID(e.serverNodeUUID)
                    }
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
                    if(e.retryAttempt){
                        retry(attempt:e.retryAttempt)
                    }
                    if(execdata.retryExecution){
                        retriedExecution{
                            execution(execdata.retryExecution)
                        }
                    }
                }
            }
        }
    }
    /**
     * Render execution list json given a List of executions, and a builder delegate
     * @param execlist list of Maps containing [execution:Execution, href: URL to execution, status: rendered status text, summary: rendered summary text]
     */
    public def renderExecutionsJson(execlist, paging = [:], delegate){
        def execAttrs = [count: execlist.size()]
        boolean isSingle=paging.single && execlist.size()==1
        if (paging) {
            execAttrs.putAll(paging)
        }
        def execarr= execlist.collect { Map execdata ->

                def href=execdata.href
                def status=execdata.status
                def summary=execdata.summary
                def Execution e = Execution.get(execdata.execution.id)
                def execMap=[
                        /** attributes   **/
                        id: e.id,
                        href: href,
                        permalink: execdata.permalink,
                        status: status,
                        project: e.project
                ]
                /** elements   */
                execMap.user=(e.user)
                execMap.'date-started'=[unixtime: e.dateStarted.time, date: w3cDateValue(e.dateStarted)]
                if (null != e.dateCompleted) {
                    execMap.'date-ended'=[unixtime: e.dateCompleted.time, date:w3cDateValue(e.dateCompleted)]
                }
                if (e.cancelled) {
                    execMap.abortedby=(e.abortedby ? e.abortedby : e.user)
                }
                if (e.scheduledExecution) {
                    def jobparams = [id: e.scheduledExecution.extid]
                    if (e.scheduledExecution.totalTime >= 0 && e.scheduledExecution.execCount > 0) {
                        def long avg = Math.floor(e.scheduledExecution.totalTime / e.scheduledExecution.execCount)
                        jobparams.averageDuration = avg
                    }
                    execMap.job=jobparams
                    execMap.job.name=(e.scheduledExecution.jobName)
                    execMap.job.group=(e.scheduledExecution.groupPath ?: '')
                    execMap.job.project=(e.scheduledExecution.project)
                    execMap.job.description=(e.scheduledExecution.description)
                    if(e.argString){
                        execMap.job.options=FrameworkService.parseOptsFromString(e.argString)
                    }
                    execMap.job.href=apiHrefForJob(e.scheduledExecution)
                    execMap.job.permalink=guiHrefForJob(e.scheduledExecution)
                }
                execMap.description=(summary)
                execMap.argstring=(e.argString)
                if(e.serverNodeUUID){
                    execMap.serverUUID=(e.serverNodeUUID)
                }
                if(e.succeededNodeList){
                    execMap.successfulNodes=e.succeededNodeList.split(',')
                }
                if(e.failedNodeList){
                    execMap.failedNodes=e.failedNodeList.split(',')
                }
                if(e.retryAttempt){
                    execMap.retryAttempt=e.retryAttempt
                }
                if(execdata.retryExecution){
                    execMap.retriedExecution=execdata.retryExecution
                }
                execMap
            }

        if(!isSingle) {
            delegate.'paging' = execAttrs
            delegate.'executions' = execarr
        }else{
            execarr[0].each{k,v->
                delegate[k]=v
            }
        }
    }

    def w3cDateValue(Date date) {
        SimpleDateFormat dateFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
        dateFormater.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormater.format(date);
    }

    String apiHrefForJob(def scheduledExecution) {
        return grailsLinkGenerator.link(controller: 'scheduledExecution',
                id: scheduledExecution.extid,
                params: [api_version:ApiRequestFilters.API_CURRENT_VERSION],
                absolute: true)
    }
    String guiHrefForJob(def scheduledExecution) {
        return grailsLinkGenerator.link(controller: 'scheduledExecution',
                action:"show",
                id: scheduledExecution.extid,
                params: [project:scheduledExecution.project],
                absolute: true)
    }
    String apiHrefForExecution(Execution execution) {
        return grailsLinkGenerator.link(controller: 'execution', id: execution.id,
                params: [api_version: ApiRequestFilters.API_CURRENT_VERSION],
                absolute: true)
    }
    String guiHrefForExecution(Execution execution) {
        return grailsLinkGenerator.link(
                controller: 'execution',
                id: execution.id,
                action: 'show',
                params: [project: execution.project],
                absolute: true
        )
    }
}
