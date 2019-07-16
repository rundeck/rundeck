package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import com.dtolabs.rundeck.core.execution.ExecutionNotFound
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileLoader
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileLoaderService
import com.dtolabs.rundeck.server.authorization.AuthConstants
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import rundeck.Execution
import rundeck.ScheduledExecution
import rundeck.services.execution.logstorage.AuthorizingExecutionFileLoaderService
import rundeck.services.execution.logstorage.ResolvedExecutionFileLoaderService

@Transactional
@CompileStatic
class AuthedLogFileLoaderService implements AuthorizingExecutionFileLoaderService {
    LogFileStorageService logFileStorageService
    FrameworkService frameworkService

    @Override
    ExecutionFileLoader requestFileLoad(
            final AuthContext auth,
            final ExecutionReference e,
            final String filetype,
            final boolean performLoad
    ) throws ExecutionNotFound {
        long exid = Long.valueOf(e.id)
        Execution exec = Execution.get(exid)
        if (!exec) {
            throw new ExecutionNotFound("Execution not found", e.id, e.project)
        }
        ScheduledExecution se = exec.scheduledExecution
        def isAuth = frameworkService.authorizeProjectExecutionAny(
                auth,
                exec,
                [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW]
        )
        if (!isAuth) {
            throw new ExecutionNotFound("Execution not found", e.id, e.project)
        }
        return logFileStorageService.requestFileLoad(e, filetype, performLoad)
    }

    ExecutionFileLoaderService serviceWithAuth(final UserAndRolesAuthContext userAndRolesAuthContext) {
        new ResolvedExecutionFileLoaderService(
                authContext: userAndRolesAuthContext,
                authorizingExecutionFileLoaderService: this,
                delegate: logFileStorageService
        )
    }
}
