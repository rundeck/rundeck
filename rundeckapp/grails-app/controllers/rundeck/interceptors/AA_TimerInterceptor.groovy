package rundeck.interceptors

import com.codahale.metrics.MetricRegistry
import org.grails.web.util.WebUtils
import org.rundeck.app.web.RequestIdProvider

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse


class AA_TimerInterceptor {
    static final Logger logger = LoggerFactory.getLogger('org.rundeck.web.requests')
    static requests=[:]
    public static final String _TIMER = 'AA_TimerFilters._TIMER'
    public static final String _METRICS_TIMER = 'AA_TimerFilters._METRICS_TIMER'
    public static final String _TIMER_ITEM = 'AA_TimerFilters._timer_item'
    public static final String _REPORTS = 'AA_TimerFilters._reports'
    public static final String _REQ_URI = 'AA_TimerFilters._req_uri'
    def MetricRegistry metricRegistry

    int order = HIGHEST_PRECEDENCE

    /**
     * Mark recording request for ident
     * @param request
     * @param ident
     * @return
     */
    static record(request, ident) {
        if(!request[_REPORTS]){
            request[_REPORTS]=new HashSet()
        }
        request[_REPORTS] << ident
    }

    static report(request,Closure clos) {
        request[_REPORTS].each{ident->
            clos.call([id:ident,duration: request[_TIMER_ITEM]?.get(ident) ?: 0])
        }
        request[_REPORTS]=null
    }

    /**
     * Record duration for an ident
     * @param request
     * @param ident
     * @param ms
     * @return
     */
    static time(request, ident,long ms){
        if (!request[_TIMER_ITEM]) {
            request[_TIMER_ITEM] = [(ident):ms]
        }else{
            request[_TIMER_ITEM][ident]= ms
        }
        record(request,ident)
    }
    static clear(){
    }

    AA_TimerInterceptor() {
        matchAll()
    }

    boolean before() {
        request[_TIMER]=System.currentTimeMillis()
        request[_METRICS_TIMER]= metricRegistry.timer(MetricRegistry.name('rundeck.web.requests','requestTimer')).time()
        def ident= (request.getAttribute(WebUtils.FORWARD_REQUEST_URI_ATTRIBUTE) ?: request.getRequestURI())
        request[_REQ_URI]=ident
        record(request, ident)
        true
    }

    boolean after() { true }

    void afterView() {
        // no-op
    }

    static def afterRequest(HttpServletRequest request, HttpServletResponse response, session) {
        request[_METRICS_TIMER].stop()
        def duration = System.currentTimeMillis() - request[_TIMER]
        time(request, request[_REQ_URI], duration)
        report(request) {Map data->
            def map = [uri: data.id,
                       duration: data.duration,
                       remoteUser: request.remoteUser ?: request.authenticatedUser,
                       remoteHost: request.remoteHost,
                       requestId: request.getAttribute(RequestIdProvider.HTTP_ATTRIBUTE_NAME),
                       userAgent: request.getHeader('User-Agent') ?: '-',
                       authToken: (request.authenticatedToken ? 'token' : 'form'),
                       method: request.method,
                       secure: request.isSecure() ? 'https' : 'http',
                       contentType: response.isCommitted()?response.getContentType():null,
                       project: request.getParameterValues('project')?.join(',')?:request.getAttribute('project')?:'?'
            ]
            map.findAll {it.value!=null}.each{ MDC.put(it.key,it.value?.toString())}
            try{
                logger.info(map.toString())
            }finally {
                MDC.clear()
            }
        }
    }
}
