package com.dtolabs.rundeck.server

import grails.web.mvc.FlashScope
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.web.filter.OncePerRequestFilter

import javax.servlet.FilterChain
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import javax.servlet.http.HttpSession

class SynchronizerTokensHolderFilter extends OncePerRequestFilter {
    private final String SYNCHRONIZER_TOKENS_HOLDER = "SYNCHRONIZER_TOKENS_HOLDER"

    @Override
    void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
        def isFlashScopeEmpty = true;
        def isFlashScopeNull = false;

        HttpSession session = request.getSession(false);

        if (session != null) {
            FlashScope flashScope = getFlashScope(session);
            if (flashScope != null) {
                isFlashScopeEmpty = flashScope.isEmpty();
            } else {
                isFlashScopeNull = true;
            }
        }

        chain.doFilter(request, response);

        if (session == null) return;

        FlashScope flashScope = getFlashScope(session);
        if ((flashScope != null && (isFlashScopeNull || !isFlashScopeEmpty || !flashScope.isEmpty())) || (flashScope == null && !isFlashScopeNull)) {
            session.setAttribute(GrailsApplicationAttributes.FLASH_SCOPE, flashScope);
        }

        updateSessionVariable(session, SYNCHRONIZER_TOKENS_HOLDER);
    }

    FlashScope getFlashScope(HttpSession session) {
        if (session != null) {
            Object flashScope = session.getAttribute(GrailsApplicationAttributes.FLASH_SCOPE);
            if (flashScope instanceof FlashScope) {
                return (FlashScope) flashScope;
            }
        }
        return null;
    }

    void updateSessionVariable(HttpSession session, String attributeName) {
        Object attributeValue = session.getAttribute(attributeName);
        session.setAttribute(attributeName, attributeValue);
    }
}