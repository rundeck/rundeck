package rundeck.interceptors

/**
 * Sets nextUi=true param when cookie is set, only for certain actions
 */
class NextUiInterceptor {
    int order = HIGHEST_PRECEDENCE + 101

    NextUiInterceptor() {
        match(controller: 'scheduledExecution', action: '(update|save|edit|create|copy|createFromExecution)')
    }

    boolean before() {
        if (request.getCookies().find { it.name == 'nextUi' }?.value == 'true' && params.nextUi != 'false') {
            params.nextUi = true
        }
        return true
    }

    boolean after() {
        return true
    }

    void afterView() {
        // no-op
    }
}
