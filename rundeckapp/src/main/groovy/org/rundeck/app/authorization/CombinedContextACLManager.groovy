package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.providers.ValidatorFactory
import com.dtolabs.rundeck.core.common.FrameworkProjectMgr
import org.rundeck.app.acl.*

/**
 * manager that uses defers to ContextACLManager for system files, builds filesystem for project files
 */
class CombinedContextACLManager extends BaseContextACLManager<AppACLContext> {
    ContextACLManager<AppACLContext> systemACLManager
    FrameworkProjectMgr projectManager
    ValidatorFactory validatorFactory

    @Override
    protected ACLFileManager createManager(final AppACLContext context) {
        if (context == AppACLContext.system()) {
            return systemACLManager.forContext(context)
        }
        return new ListenableACLFileManager(
            ACLFSFileManager.builder()
                            .directory(new File(projectManager.getBaseDir(), context.project + '/acls'))
                            .validator(
                                validatorFactory.forProjectOnly(context.getProject())
                            )
                            .build()

        )
    }
}
