package com.dtolabs.rundeck.app.gui

import com.dtolabs.rundeck.core.authorization.AuthContextEvaluator
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil
import com.dtolabs.rundeck.core.authorization.Decision
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import grails.web.mapping.LinkGenerator
import groovy.transform.CompileStatic
import org.rundeck.app.gui.AuthMenuItem
import org.rundeck.core.auth.AuthConstants
import org.springframework.beans.factory.annotation.Autowired

@CompileStatic
class SystemReportMenuItem implements AuthMenuItem {
    @Autowired
    LinkGenerator grailsLinkGenerator
    @Autowired
    AuthContextEvaluator rundeckAuthContextEvaluator

    final MenuType type = MenuType.SYSTEM_CONFIG
    final String titleCode = 'gui.menu.SystemInfo'
    final String title = 'System Report'

    @Override
    String getHref() {
        return grailsLinkGenerator.link(
            action: "menu",
            controller: "systemInfo"
        )
    }

    @Override
    boolean isEnabled(final UserAndRolesAuthContext auth) {
        return rundeckAuthContextEvaluator.
            authorizeApplicationResourceAny(
                auth,
                AuthConstants.RESOURCE_TYPE_SYSTEM,
                [AuthConstants.ACTION_READ, AuthConstants.ACTION_ADMIN]
            )
    }
}
