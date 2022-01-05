package org.rundeck.app.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Utility methods provided for web requests
 */
public interface WebUtilService {


    /**
     * Render error in either JSON or XML format, depending on expected response
     *
     * @param response
     * @param error
     */
    void renderErrorFormat(HttpServletResponse response, Map<String, Object> error);

    void renderErrorJson(HttpServletResponse response, Map<String, Object> error);

    void renderErrorXml(HttpServletResponse response, Map<String, Object> error);

    /**
     * Require API request with a minimum API version
     *
     * @param request
     * @param response
     * @param min
     */
    boolean requireApi(HttpServletRequest request, HttpServletResponse response, int min);

    /**
     * Require API request
     *
     * @param request
     * @param response
     */
    boolean requireApi(HttpServletRequest request, HttpServletResponse response);

    /**
     * Require API version minimum
     *
     * @param request
     * @param response
     * @param min
     */
    boolean requireVersion(HttpServletRequest request, HttpServletResponse response, int min);

    /**
     * Parse XML or JSON input formatted data, and handle with appropriate closure.  If the input format is not
     * supported, or there is an error parsing the input, an error response is sent, and false is returned.
     *
     * @param request  request
     * @param response response
     * @param handlers handler map, using keys 'xml' or 'json'.
     * @return true if parsing was successful, false if an error occurred and a response has already been sent
     */
    public boolean parseJsonXmlWith(
            HttpServletRequest request,
            HttpServletResponse response,
            Map<String, Consumer<Object>> handlers
    );


    /**
     * Require request to be a certain format, returns false if not valid and error response is already sent
     *
     * @param request        request
     * @param response       response
     * @param allowed        allowed formats or mime-types
     * @param responseFormat response format to send ('xml' or 'json') if request is not valid, or null to use default
     * @return true if valid, false otherwise
     */
    boolean requireRequestFormat(
            HttpServletRequest request, HttpServletResponse response, List<String> allowed, String responseFormat
    );

    /**
     * Require request to be a certain format, returns false if not valid and error response is already sent
     *
     * @param request  request
     * @param response response
     * @param allowed  allowed formats or mime-types
     * @return true if valid, false otherwise
     */
    boolean requireRequestFormat(
            HttpServletRequest request, HttpServletResponse response, List<String> allowed
    );

    /**
     * @param request request
     * @return api version or null if not an API request
     */
    Integer getRequestApiVersion(HttpServletRequest request);

    /**
     * @param request
     * @return true if request is API request
     */
    boolean isApiRequest(HttpServletRequest request);

    /**
     * Return an error response
     *
     * @param request
     * @param response
     * @param code
     * @param status
     * @param args
     */
    void respondError(
            final HttpServletRequest request,
            final HttpServletResponse response,
            String code,
            int status,
            List<?> args
    );

    /**
     * Determine appropriate response format based on allowed formats or request format. If the requested response type
     * is in the allowed formats it is returned, otherwise if a default format is specified it is used.  If the response
     * format is not in the allowed formats and no default is specified, the request content type format is returned.
     *
     * @param request   request
     * @param response  response
     * @param allowed   list of allowed formats
     * @param defformat default format, or null to use the request format
     * @return format name
     */
    String extractResponseFormat(
            HttpServletRequest request, HttpServletResponse response,
            List<String> allowed, String defformat
    );

    /**
     * Determine appropriate response format based on allowed formats or request format. If the requested response type
     * is in the allowed formats it is returned, otherwise if a default format is specified it is used.  If the response
     * format is not in the allowed formats and no default is specified, the request content type format is returned.
     *
     * @param request  request
     * @param response response
     * @param allowed  list of allowed formats
     * @return format name
     */
    String extractResponseFormat(
            HttpServletRequest request, HttpServletResponse response,
            List<String> allowed
    );

    /**
     * Output response content
     *
     * @param response
     * @param contentType
     * @param output
     */
    void respondOutput(HttpServletResponse response, String contentType, String output);

}
