package rundeck.interceptors

import rundeck.services.feature.FeatureService

/**
 * Handles nextUi mode for capable pages:
 * - Marks pages as nextUi capable via request attribute
 * - Sets params.nextUi based on system flag and user cookie preference
 */
class NextUiInterceptor {
    int order = HIGHEST_PRECEDENCE + 101

    FeatureService featureService

    NextUiInterceptor() {
        // Single source of truth for all nextUi-capable pages
        match(controller: 'scheduledExecution', action: '(update|save|edit|create|copy|createFromExecution)')
        match(controller: 'menu', action: '(jobs|home)')
    }

    boolean before() {
        // Always mark this page as nextUi capable (since match() determines capability)
        request.setAttribute('nextUiCapable', true)

        // Determine nextUi state based on system flag and user cookie preference
        def cookie = request.getCookies()?.find { it.name == 'nextUi' }
        def cookieValue = cookie?.value
        def systemEnabled = featureService.featurePresent('nextUiMode')

        if (cookieValue == 'false') {
            // User explicitly disabled - respect their preference
            params.nextUi = false
        } else if (cookieValue == 'true') {
            // User explicitly enabled
            params.nextUi = true
        } else if (systemEnabled) {
            // No cookie set, but system flag is enabled - default to true
            params.nextUi = true
        }
        // Otherwise, params.nextUi stays unset (defaults to current UI)

        return true
    }

    boolean after() {
        return true
    }

    void afterView() {
        // no-op
    }
}
