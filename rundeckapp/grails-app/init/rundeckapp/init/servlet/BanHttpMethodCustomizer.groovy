package rundeckapp.init.servlet

import jakarta.servlet.DispatcherType
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.FilterConfig
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Validates HTTP methods and rejects disallowed methods like TRACE
 * 
 * Grails 7/Jetty 12 Migration Note: Converted from Handler.Wrapper to Servlet Filter
 * to avoid ServletApiRequest.getRequest() NPE issues. Servlet Filters operate at the
 * Jakarta EE layer after Request objects are fully initialized.
 * 
 * TEMPORARILY DISABLED to debug ServletApiRequest NPE issue
 */
// @Configuration
class BanHttpMethodCustomizer_DISABLED {

    // @Bean
    FilterRegistrationBean<HttpMethodFilter> httpMethodFilter_DISABLED() {
        FilterRegistrationBean<HttpMethodFilter> registration = new FilterRegistrationBean<>()
        registration.setFilter(new HttpMethodFilter(['TRACE']))
        registration.addUrlPatterns('/*')
        registration.setOrder(1) // Run early in filter chain
        registration.setDispatcherTypes(
            DispatcherType.REQUEST,
            DispatcherType.FORWARD,
            DispatcherType.INCLUDE,
            DispatcherType.ERROR
        )
        return registration
    }
}

/**
 * Servlet Filter that rejects banned HTTP methods
 */
class HttpMethodFilter implements Filter {

    final List<String> banMethods

    HttpMethodFilter(List<String> methods) {
        this.banMethods = methods*.toUpperCase()
    }

    @Override
    void init(FilterConfig filterConfig) throws ServletException {
        // No initialization needed
    }

    @Override
    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request
            HttpServletResponse httpResponse = (HttpServletResponse) response
            
            String method = httpRequest.getMethod()
            if (banMethods.contains(method?.toUpperCase())) {
                httpResponse.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method Not Allowed")
                return
            }
        }
        
        chain.doFilter(request, response)
    }

    @Override
    void destroy() {
        // No cleanup needed
    }
}
