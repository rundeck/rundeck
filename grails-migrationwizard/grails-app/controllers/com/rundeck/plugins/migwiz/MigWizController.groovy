package com.rundeck.plugins.migwiz

import com.dtolabs.rundeck.core.authorization.AuthContextProvider
import com.rundeck.plugins.migwiz.rba.RBAInstanceData
import org.rundeck.core.auth.app.RundeckAccess
import org.rundeck.core.auth.web.RdAuthorizeProject

import javax.security.auth.Subject

class MigWizController {

    MigrationWizardService migrationWizardService
    AuthContextProvider rundeckAuthContextProvider


    def index() {}


    @RdAuthorizeProject(RundeckAccess.Project.AUTH_APP_EXPORT)
    def migrateProjectToRBA(RBAInstanceData instance) {

        String project = params.project

        def authContext = rundeckAuthContextProvider.getAuthContextForSubjectAndProject(
            getSubject(),
            project
        )

        return migrationWizardService.migrateProjectToRBA(project, instance, authContext)
    }


    Subject getSubject() {
        def subject = session.getAttribute('subject')
        if (subject instanceof Subject) {
            return subject
        }
        throw new IllegalStateException("no subject found in session")
    }
}
