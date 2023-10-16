package org.rundeck.web

import com.dtolabs.rundeck.app.api.ApiVersions
import com.dtolabs.rundeck.core.config.FeatureService
import com.dtolabs.rundeck.core.config.Features
import grails.artefact.controller.support.ResponseRenderer
import grails.converters.JSON
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder
import org.grails.web.converters.exceptions.ConverterException
import org.grails.web.util.GrailsApplicationAttributes
import org.rundeck.app.web.WebUtilService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.web.servlet.ModelAndView

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.Callable
import java.util.function.BiConsumer
import java.util.function.Consumer

/**
 * Utility methods for handling requests, responses and api results
 */
@CompileStatic
class WebUtil implements WebUtilService, ResponseRenderer {
    public static final String APPLICATION_XML_CONTENT_TYPE = 'application/xml'
    public static final String JSON_CONTENT_TYPE = 'application/json'
    @Autowired
    MessageSource messageSource
    @Autowired
    FeatureService featureService
    static interface ResponseErrorHandler extends BiConsumer<HttpServletResponse, Map<String, Object>> {}
    Map<String, ResponseErrorHandler> respHandlers = new HashMap<>(
        [
            json : this.&renderErrorJson as ResponseErrorHandler,
            text : this.&renderErrorText as ResponseErrorHandler
        ]
    )

    void renderErrorText(HttpServletResponse response, Map<String, Object> err) {
        def status = err.status
        if (status instanceof Integer) {
            response.setStatus(status)
        }
        appendResponseOutput(response, renderErrorText(err))
    }

    @Override
    void renderErrorFormat(HttpServletResponse response, Map<String, Object> error) {
        def handlers = new HashMap(respHandlers)
        if(featureService.featurePresent(Features.LEGACY_XML,false)){
            handlers.put('xml', this.&renderErrorXml as ResponseErrorHandler)
        }
        def eformat = error.format.toString()
        def rformat = response.format
        def respFormat = eformat && respHandlers[eformat] ? eformat :
                rformat && respHandlers[rformat] ? rformat : 'json'
        respHandlers[respFormat].accept(response, error)
    }

    def appendResponseOutput(HttpServletResponse response, String output) {
        response.outputStream << output
    }

    String renderErrorText(messages, String code = null) {
        if (!messages) {
            return messageSource.getMessage("api.error.unknown", null, "api.error.unknown", null)
        }
        if (messages instanceof List) {
            return messages.join("\r\n")
        } else if (messages instanceof Map && messages.message) {
            return messages.message
        } else if (messages instanceof Map && messages.code) {
            return messageSource.getMessage(
                messages.code.toString(),
                messages.args ? messages.args as Object[] : null,
                messages.code.toString(),
                null
            )
        }
        return messages.toString()
    }

    void renderErrorXml(HttpServletResponse response, Map<String, Object> error) {
        def status = error.status
        if (status instanceof Integer) {
            response.setStatus(status)
        }
        respondOutput(response, APPLICATION_XML_CONTENT_TYPE, renderErrorXml(error, error.code.toString()))
    }

    @CompileDynamic
    String renderErrorXml(Object messages, String code = null, builder = null) {
        def writer = new StringWriter()
        def xml
        if (!builder) {
            xml = new MarkupBuilder(writer)
        } else {
            xml = builder
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
                    } else if (messages instanceof Map && messages.code) {
                        delegate.'message'(
                            messages.message ?:
                            messageSource.getMessage(
                                messages.code,
                                messages.args ? messages.args as Object[] : null,
                                messages.code,
                                null
                            )
                        )
                    } else if (messages instanceof Map && messages.message) {
                        delegate.'message'(messages.message)
                    }
                }
            }
        }
        if (!builder) {
            return writer.toString()
        }
    }


    void renderErrorJson(HttpServletResponse response, Map<String, Object> error) {
        def status = error.status
        if (status instanceof Integer) {
            response.setStatus(status)
        }
        respondOutput(response, JSON_CONTENT_TYPE, renderErrorJson(error, error.code.toString()))
    }

    Map<String,Object> createErrorMap(Object messages, String code = null) {
        Map<String, Object> result = new HashMap<>()
        result.putAll(
            [
                error     : true,
                apiversion: ApiVersions.API_CURRENT_VERSION,
            ]
        )
        result.errorCode = code ?: 'api.error.unknown'
        if (!messages) {
            result.'message' = messageSource.getMessage("api.error.unknown", null, "api.error.unknown", null)
        }
        if (messages instanceof List) {
            result.messages = messages
        } else if (messages instanceof Map && messages.code) {
            result.message = (
                messages.message ?:
                messageSource.getMessage(
                    messages.code.toString(),
                    messages.args ? messages.args as Object[] : null,
                    messages.code.toString(),
                    null
                )
            )
        } else if (messages instanceof Map && messages.message) {
            result.put('message', messages.message.toString())
        }
        return result
    }
    String renderErrorJson(Object messages, String code = null) {
        return (createErrorMap(messages,code) as JSON).toString()
    }

    void respondOutput(HttpServletResponse response, String contentType, String output) {
        response.setContentType(contentType)
        response.setCharacterEncoding('UTF-8')
        response.setHeader("X-Rundeck-API-Version", ApiVersions.API_CURRENT_VERSION.toString())
        def out = response.outputStream
        out << output
        out.flush()
    }

    @Override
    boolean requireApi(
        final HttpServletRequest request,
        final HttpServletResponse response,
        final int min = ApiVersions.API_MIN_VERSION
    ) {
        if (!getRequestApiVersion(request)) {
            //not a /api URL
            response.sendError(HttpServletResponse.SC_NOT_FOUND)

            return false
        }
        if (!requireVersion(request, response, min)) {
            return false
        }

        true
    }

    @Override
    public Integer getRequestApiVersion(HttpServletRequest request) {
        def attribute = request.getAttribute('api_version')
        if (attribute instanceof Integer) {
            return attribute
        }
        return null
    }
    @Override
    public boolean isApiRequest(HttpServletRequest request) {
        return getRequestApiVersion(request)!=null
    }

    /**
     * Obtains the ModelAndView for the currently executing controller
     *
     * @return The ModelAndView
     */
    ModelAndView getModelAndView() {
        (ModelAndView)currentRequestAttributes().getAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, 0)
    }

    /**
     * Sets the ModelAndView of the current controller
     *
     * @param mav The ModelAndView
     */
    void setModelAndView(ModelAndView mav) {
        currentRequestAttributes().setAttribute(GrailsApplicationAttributes.MODEL_AND_VIEW, mav, 0)
    }

     void respondError(
        final HttpServletRequest request,
        final HttpServletResponse response,
        String code,
        int status,
        List args
    ) {
        if (isApiRequest(request)) {
            renderErrorFormat(
                response, new HashMap<String, Object>(
                [
                    status: status,
                    code  : code,
                    args  : args.toArray()
                ]
            )
            )
        } else {
            request.setAttribute('errorCode', code)
            request.setAttribute('errorArgs', args.toArray())
            response.status = status
            if (status in [HttpServletResponse.SC_NOT_FOUND, HttpServletResponse.SC_METHOD_NOT_ALLOWED]) {
                render(view: "/$status")
            } else {
                render(view: "/common/error")
            }
        }
    }
    boolean requireVersion(HttpServletRequest request, HttpServletResponse response, int min, int max = 0) {
        if (getRequestApiVersion(request) < min) {
            renderErrorFormat(
                response, [
                status: HttpServletResponse.SC_BAD_REQUEST,
                code  : 'api.error.api-version.unsupported',
                args  : [getRequestApiVersion(request), request.forwardURI, "Minimum supported version: " + min]
            ]
            )
            return false
        }
        if (max > 0 && getRequestApiVersion(request) > max) {
            renderErrorFormat(
                response, [
                status: HttpServletResponse.SC_BAD_REQUEST,
                code  : 'api.error.api-version.unsupported',
                args  : [getRequestApiVersion(request), request.forwardURI, "Maximum supported version: " + max]
            ]
            )
            return false
        }
        return true
    }

    public String extractResponseFormat(
        HttpServletRequest request,
        HttpServletResponse response,
        List<String> allowed,
        String defformat = null
    ) {
        def defFormatEval = defformat ?: request.format
        return ((response.format in allowed) ? response.format : (defFormatEval in allowed ? defFormatEval : null))
    }
    /**
     * Require request to be a certain format, returns false if not valid and error response is already sent
     * @param request request
     * @param response response
     * @param allowed allowed formats or mime-types
     * @param responseFormat response format to send ('xml' or 'json') if request is not valid, or null to use default
     * @return true if valid, false otherwise
     */
    boolean requireRequestFormat(
        HttpServletRequest request,
        HttpServletResponse response,
        List<String> allowed,
        String responseFormat = null
    ) {
        def contentType = request.getHeader("Content-Type")
        def test = request.format in allowed || (contentType && (extractMimeType(contentType) in allowed))
        if (!test) {
            //bad request
            renderErrorFormat(
                response,
                [
                    status: HttpServletResponse.SC_BAD_REQUEST,
                    code  : "api.error.invalid.request",
                    args  : ["Expected request content to be one of allowed formats: [" + allowed.join(', ') +
                             "], " +
                             "but was: " +
                             "${contentType}"],
                    format: responseFormat
                ]
            )
        }
        test
    }

    String extractMimeType(String s) {
        return s.contains(';') ? s.split(';')[0].trim() : s;
    }


    /**
     * Parse XML or JSON input formatted data, and handle with appropriate closure.  If the input format is not
     * supported, or there is an error parsing the input, an error response is sent, and false is returned.
     * @param request request
     * @param response response
     * @param handlers handler map, using keys 'xml' or 'json'.
     * @return true if parsing was successful, false if an error occurred and a response has already been sent
     */
    public boolean parseJsonXmlWith(
        HttpServletRequest request,
        HttpServletResponse response,
        Map<String, Consumer<Object>> handlers
    ) {
        def respFormat = extractResponseFormat(request, response, ['xml', 'json'])
        if (!requireRequestFormat(request, response, ['xml', 'json'], respFormat)) {
            return false
        }
        String error
        Consumer<Object> handler = handlers[respFormat]
        if (!handler) {
            error = "Unexpected content type: ${request.getHeader('Content-Type')}"
        } else if (respFormat == 'json') {
            try {
                Object parsed = request.JSON

                if (!parsed) {
                    error = "Could not parse JSON"
                } else {
                    handler.accept(parsed)
                }
            } catch (ConverterException e) {
                error = e.message + (e.cause ? ": ${e.cause.message}" : '')
            }

        } else {
            try {
                handler.accept(request.XML)
            } catch (ConverterException e) {
                error = e.message + (e.cause ? ": ${e.cause.message}" : '')
            }
        }

        if (error) {
            renderErrorFormat(
                response, new HashMap<>(
                [
                    status : HttpServletResponse.SC_BAD_REQUEST,
                    message: error,
                    format : respFormat
                ]
            )
            )
            return false
        }
        return true
    }


}
