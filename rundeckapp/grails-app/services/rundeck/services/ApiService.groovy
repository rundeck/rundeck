/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.authorization.Validation
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.converters.JSON
import grails.transaction.Transactional
import grails.web.JSONBuilder
import groovy.xml.MarkupBuilder
import org.apache.commons.lang.RandomStringUtils
import org.grails.web.converters.exceptions.ConverterException
import org.rundeck.util.Sizes
import rundeck.AuthToken
import rundeck.Execution
import rundeck.User
import com.dtolabs.rundeck.app.api.ApiVersions

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.text.SimpleDateFormat
import java.time.Clock

class ApiService {
    public static final String TEXT_XML_CONTENT_TYPE = 'text/xml'
    public static final String APPLICATION_XML_CONTENT_TYPE = 'application/xml'
    public static final String JSON_CONTENT_TYPE = 'application/json'
    public static final String XML_API_RESPONSE_WRAPPER_HEADER = "X-Rundeck-API-XML-Response-Wrapper"
    def messageSource
    def grailsLinkGenerator
    def frameworkService
    def configurationService
    def userService

    public static final Map<String,String> HTTP_METHOD_ACTIONS = Collections.unmodifiableMap (
            POST: AuthConstants.ACTION_CREATE,
            PUT: AuthConstants.ACTION_UPDATE,
            GET: AuthConstants.ACTION_READ,
            DELETE: AuthConstants.ACTION_DELETE
    )
    private String genRandomString() {
        return RandomStringUtils.random(32, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789")
    }

    Clock systemClock = Clock.systemUTC()
    /**
     * Generate the expiration date for a token given the duration string, and
     * duration max string
     * @param tokenDuration duration string
     * @param maxDuration optional maximum duration time, if null then no maximum
     * @return [date:Date] date is UTC time, or [error:'format'] if the input duration is not valid or [error:'max']
     * if it exceeds the
     * maximum
     */
    Map generateTokenExpirationDate(Integer tokenDuration, Integer maxDuration = null) {
        int useTokenTime = maxDuration ?: 0
        boolean max = false
        if (tokenDuration) {
            if (tokenDuration <= useTokenTime || useTokenTime < 1) {
                useTokenTime = tokenDuration
            } else {
                max = true
            }
        }
        def newDate = null;
        if (useTokenTime > 0) {
            def currentDate = systemClock.instant()
            newDate = Date.from(currentDate.plusSeconds(useTokenTime))
        }
        [date: newDate, max: max]
    }
    /**
     * Generate a new unique auth token for the user and specific groups and return it
     * @param u
     * @return
     */
    AuthToken generateAuthToken(String ownerUsername, User u, Set<String> roles, Date expiration) {

        String newtoken = genRandomString()
        while (AuthToken.findByToken(newtoken) != null) {
            newtoken = genRandomString()
        }
        def uuid = UUID.randomUUID().toString()
        AuthToken token = new AuthToken(
                token: newtoken,
                authRoles: AuthToken.generateAuthRoles(roles),
                user: u,
                expiration: expiration,
                uuid: uuid,
                creator: ownerUsername
        )

        if (token.save()) {
            log.info(
                    "GENERATE TOKEN: ID:${uuid} creator:${ownerUsername} username:${u.login} roles:"
                            + "${token.authRoles} expiration:${expiration}"
            )
            return token
        } else {
            throw new Exception("Failed to save token for User ${u.login}")
        }
    }

    /**
     * Return the resource definition for a job for use by authorization checks, using parameters as input
     * @param se
     * @return
     */
    def Map authResourceForUserToken(String username, Set<String> roles) {
        return AuthorizationUtil.resource(
                AuthConstants.TYPE_APITOKEN,
                [username: username, roles: AuthToken.generateAuthRoles(roles)]
        )
    }
    /**
     * Find a token by UUID and creator
     */
    AuthToken findUserTokenId(
            String creator,
            String id
    )
    {
        AuthToken.findByUuidAndCreator(id, creator)
    }
    /**
     * Find a token by UUID and creator
     */
    List<AuthToken> findUserTokensCreator(
            String creator
    )
    {
        AuthToken.findAllByCreator(creator)
    }
    /**
     * Find a token by UUID
     */
    AuthToken findTokenId(String id) {
        AuthToken.findByUuid(id)
    }
    /**
     * Find a token by UUID
     */
    AuthToken findUserTokenValue(String token) {
        AuthToken.findByToken(token)
    }
    /**
     * Generate an auth token
     * @param authContext user's own auth context
     * @param tokenTime time value for token expiration
     * @param tokenTimeUnit time unit for token expiration (h,m,s)
     * @param username owner name of token
     * @param tokenRoles role list for token, or null to use all owner roles (user token only)
     * @return
     */
    AuthToken generateUserToken(
            UserAndRolesAuthContext authContext,
            Integer tokenTimeSeconds,
            String username,
            Set<String> roles
    )
    {
        //check auth to edit profile
        //default to current user profile
        def createTokenUser = authContext.username

        def selfAuth = false
        def serviceAuth = false
        //admin auth allows generate of any user token with anhy roles
        def adminAuth = hasTokenAdminAuth(authContext)
        if (!adminAuth) {
            //service auth allows generate of any user token with additional service roles
            serviceAuth = hasTokenServiceGenerateAuth(authContext)
            if (!serviceAuth) {
                //self auth allows generate of self-owned token with any subset of self-owned roles
                selfAuth = hasTokenUserGenerateAuth(authContext)
            }
        }
        if (!(adminAuth || serviceAuth || selfAuth)) {
            throw new Exception("Unauthorized: generate API token")
        }
        if (username) {
            if (adminAuth || serviceAuth) {
                createTokenUser = username
            } else if (username != authContext.username) {
                throw new Exception("Unauthorized: generate API token")
            }
        }
        def userRoles = authContext.roles


        if (serviceAuth && roles) {
            //any roles not implicitly allowed by user's access level
            def extraRoles = roles - userRoles
            //authorize any extra roles
            if (extraRoles) {
                if (!frameworkService.authorizeApplicationResource(
                        authContext,
                        authResourceForUserToken(createTokenUser, extraRoles),
                        AuthConstants.ACTION_CREATE
                )) {
                    throw new Exception("Unauthorized: create API token for $createTokenUser with roles: $roles")
                }
            }
        } else if (!adminAuth) {
            if (roles && !userRoles.containsAll(roles)) {
                throw new Exception("Unauthorized: create API token for $createTokenUser with roles: $roles")
            }
        }
        if (!roles) {
            if (username != authContext.username) {
                throw new Exception("Cannot create API token for $username: Roles are required")
            } else if (username == authContext.username) {
                //default to user's own roles
                roles = authContext.roles
            }
        }

        Integer maxTokenDuration = maxTokenDurationConfig()
        def generate = generateTokenExpirationDate(tokenTimeSeconds, maxTokenDuration)
        if (generate.max) {
            throw new Exception("Duration exceeds maximum allowed: " + maxTokenDuration)
        }
        Date newDate = generate.date

        User u = userService.findOrCreateUser(createTokenUser)
        if (!u) {
            throw new Exception("Couldn't find user: ${createTokenUser}")
        }
        return generateAuthToken(authContext.username, u, roles, newDate)
    }

    public boolean hasTokenUserGenerateAuth(UserAndRolesAuthContext authContext) {
        authorizedForTokenAction(authContext, AuthConstants.GENERATE_USER_TOKEN)
    }

    public boolean hasTokenServiceGenerateAuth(UserAndRolesAuthContext authContext) {
        authorizedForTokenAction(authContext, AuthConstants.GENERATE_SERVICE_TOKEN)
    }

    private boolean authorizedForTokenAction(UserAndRolesAuthContext authContext, String action) {
        frameworkService.authorizeApplicationResourceType(
                authContext,
                AuthConstants.TYPE_APITOKEN,
                action
        )
    }

    public boolean hasTokenAdminAuth(UserAndRolesAuthContext authContext) {
        frameworkService.authorizeApplicationResourceType(
                authContext,
                AuthConstants.TYPE_APITOKEN,
                AuthConstants.ACTION_ADMIN
        ) || frameworkService.authorizeApplicationResourceType(
                authContext,
                AuthConstants.TYPE_USER,
                AuthConstants.ACTION_ADMIN
        )
    }

    public int maxTokenDurationConfig() {
        def string = configurationService.getString("api.tokens.duration.max", null)
        if (!Sizes.validTimeDuration(string)) {
            log.warn("Invalid configuration for rundeck.api.tokens.duration.max: " + string + ", using 30d")
            string = "30d"
        }
        string ? Sizes.parseTimeDuration(string) : 0
    }

    def respondOutput(HttpServletResponse response, String contentType, String output) {
        response.setContentType(contentType)
        response.setCharacterEncoding('UTF-8')
        response.setHeader("X-Rundeck-API-Version",ApiVersions.API_CURRENT_VERSION.toString())
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
                message(messageSource.getMessage(code, args as Object[], code, null))
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
        if(request.api_version < ApiVersions.V11){
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
    /**
     * TODO: remove "result" wrapper from API responses after Rundeck 2.6
     * @param recall
     * @return
     * @deprecated
     */
    def renderSuccessXml(Closure recall){
        return renderSuccessXmlUnwrapped {
            result(success: "true", apiversion: ApiVersions.API_CURRENT_VERSION) {
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
        def defFormatEval = defformat ?: request.format
        return ((response.format in allowed) ? response.format : (defFormatEval in allowed?defFormatEval:null))
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
     * Return an unauthorized response
     * @param response
     * @param code api code, default: 'api.error.item.unauthorized'
     * @param args args to message code
     * @return
     */
    def renderUnauthorized(HttpServletResponse response, List args, String code = 'api.error.item.unauthorized') {
        renderErrorFormat(
                response,
                [
                        status: HttpServletResponse.SC_FORBIDDEN,
                        code  : code,
                        args  : args
                ]
        )
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
            return messageSource.getMessage("api.error.unknown", null, "api.error.unknown", null)
        }
        if (messages instanceof List) {
            return messages.join("\r\n")
        }else if (messages instanceof Map && messages.message) {
            return messages.message
        } else if (messages instanceof Map && messages.code) {
            return messageSource.getMessage(
                messages.code,
                messages.args ? messages.args as Object[] : null,
                messages.code,
                null
            )
        }
        return messages.toString()
    }
    def renderErrorJson(messages, String code=null){
        def result=[
                error: true,
                apiversion: ApiVersions.API_CURRENT_VERSION,
        ]
        result.errorCode = code ?: 'api.error.unknown'
        if (!messages) {
            result.'message' = messageSource.getMessage("api.error.unknown", null, "api.error.unknown", null)
        }
        if (messages instanceof List) {
            result.messages=messages
        } else if (messages instanceof Map && messages.code) {
            result.message = (
                messages.message ?:
                messageSource.getMessage(
                    messages.code,
                    messages.args ? messages.args as Object[] : null,
                    messages.code,
                    null
                )
            )
        }else if (messages instanceof Map && messages.message) {
            result.message=messages.message
        }
        return (result as JSON).toString()
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
            result(error: "true", apiversion: ApiVersions.API_CURRENT_VERSION) {
                // REVIEW: disabled by grails3 merge
//                def errorprops = [:]
                def errorprops = [code: code ?: 'api.error.unknown']
                if (code) {
                    errorprops = [code: code]
                }
                delegate.'error'(errorprops) {
                    if (!messages) {
                        delegate.'message'(
                            messageSource.getMessage("api.error.unknown", null, "api.error.unknown", null)
                        )
                    }
                    if (messages instanceof List) {
                        delegate.'messages' {
                            messages.each {
                                delegate.'message'(it)
                            }
                        }
                    }else if(messages instanceof Map && messages.code){
                        delegate.'message'(
                            messages.message ?:
                            messageSource.getMessage(
                                messages.code,
                                messages.args ? messages.args as Object[] : null,
                                messages.code,
                                null
                            )
                        )
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
     * in XML, render a file as a wrapped strings specified by a 'contents' entry/element
     * @param contentString
     * @param request
     * @param response
     * @param respFormat
     * @param delegate
     * @return
     */
    void renderWrappedFileContentsXml(
            String contentString,
            String respFormat,
            delegate
    )
    {
        delegate.'contents' {
            mkp.yieldUnescaped("<![CDATA[" + contentString.replaceAll(']]>', ']]]]><![CDATA[>') + "]]>")
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
     Map jsonRenderDirlist(String path,Closure genpath,Closure genhref,List<String>dirlist){
         def json = [:]
         json.path = genpath(path)
         json.type = 'directory'
         json.href = genhref(path)
         json.resources = dirlist.collect {dirpath->
             def e = ['path':genpath(dirpath),
                      'type':dirpath.endsWith('/')?'directory':'file',
                      'href': genhref(dirpath)]
             if(!dirpath.endsWith('/')) {
                 e.name = pathName(genpath(dirpath))
             }
             return e
         }
         return json
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

    Map renderJsonAclpolicyValidation(Validation validation){
        def json = [:]
        json.valid = validation.valid
        if(!validation.valid) {
            json.policies = validation.errors.keySet().sort().collect { ident ->
                [policy: ident, errors: validation.errors[ident]]
            }
        }
        return json
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
                Execution e = execdata.execution
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

                    if(e.customStatusString){
                        customStatus(e.customStatusString)
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
                        project: e.project,
                        executionType:e.executionType
                ]
            if(execdata.customStatus){
                execMap['customStatus']=execdata.customStatus
            }
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
                params: [api_version:ApiVersions.API_CURRENT_VERSION],
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
                params: [api_version: ApiVersions.API_CURRENT_VERSION],
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

    def removeToken(final AuthToken authToken) {

        def user = authToken.user
        def creator = authToken.creator ?: user.login
        def id = authToken.uuid ?: authToken.token
        def oldAuthRoles = authToken.authRoles

        authToken.delete(flush: true)
        log.info("DELETED TOKEN ${id} (creator:$creator) User ${user.login} with roles: ${oldAuthRoles}")
    }

    /**
     * Find and remove AuthTokens created by creator that are expired
     * @param creator
     * @return
     */
    @Transactional
    def removeAllExpiredTokens(final String creator) {
        def now = Date.from(Clock.systemUTC().instant())
        def found = AuthToken.findAllByCreatorAndExpirationLessThan(creator, now)
        if (found) {
            found.each {
                it.delete()
            }
        }
        found.size()
    }

    /**
     * Find and remove all AuthTokens that are expired
     * @param creator
     * @return
     */
    @Transactional
    def removeAllExpiredTokens() {
        def now = Date.from(Clock.systemUTC().instant())
        def found = AuthToken.findAllByExpirationLessThan(now)
        if (found) {
            found.each {
                it.delete()
            }
        }
        found.size()
    }
}
