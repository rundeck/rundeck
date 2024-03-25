package org.rundeck.app.ui

import org.springframework.core.annotation.Order
import org.springframework.web.filter.GenericFilterBean
import rundeck.services.ConfigurationService

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.ServletRequest
import javax.servlet.ServletResponse

@Order(-1)
class UiVersionFilter extends GenericFilterBean {
    ConfigurationService configurationService
    @Override
    void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String cdn = configurationService.getString("gui.cdn","")
        servletRequest.setAttribute("CDN", cdn)
        filterChain.doFilter(servletRequest, servletResponse)
    }
}
