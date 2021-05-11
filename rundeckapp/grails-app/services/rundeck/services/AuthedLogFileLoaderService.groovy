package rundeck.services

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext
import org.rundeck.app.authorization.AppAuthContextEvaluator
import org.rundeck.core.auth.AuthConstants
import com.dtolabs.rundeck.core.execution.ExecutionNotFound
import com.dtolabs.rundeck.core.execution.ExecutionReference
import com.dtolabs.rundeck.core.execution.logstorage.AsyncExecutionFileLoaderService
import com.dtolabs.rundeck.core.execution.logstorage.ExecutionFileLoader
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic
import rundeck.Execution
import rundeck.services.execution.logstorage.AuthorizingExecutionFileLoaderService
import rundeck.services.execution.logstorage.ResolvedExecutionFileLoaderService

import java.util.concurrent.CompletableFuture

@Transactional
@CompileStatic
class AuthedLogFileLoaderService implements AuthorizingExecutionFileLoaderService {
    LogFileStorageService logFileStorageService
    AppAuthContextEvaluator rundeckAuthContextEvaluator

    @Override
    ExecutionFileLoader requestFileLoad(
            final AuthContext auth,
            final ExecutionReference e,
            final String filetype,
            final boolean performLoad
    ) throws ExecutionNotFound {
        checkAuth(e, auth)
        return logFileStorageService.requestFileLoad(e, filetype, performLoad)
    }

    @Override
    CompletableFuture<ExecutionFileLoader> requestFileLoadAsync(
            final AuthContext auth,
            final ExecutionReference e,
            final String filetype
    ) throws ExecutionNotFound {
        checkAuth(e, auth)
        return logFileStorageService.requestFileLoadAsync(e, filetype)
    }

    private void checkAuth(ExecutionReference e, AuthContext auth) throws ExecutionNotFound {
        long exid = Long.valueOf(e.id)
        Execution exec = Execution.get(exid)
        if (!exec) {
            throw new ExecutionNotFound("Execution not found", e.id, e.project)
        }
        def isAuth = rundeckAuthContextEvaluator.authorizeProjectExecutionAny(
                auth,
                exec,
                [AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW]
        )
        if (!isAuth) {
            throw new ExecutionNotFound("Execution not found", e.id, e.project)
        }
    }

    AsyncExecutionFileLoaderService serviceWithAuth(final UserAndRolesAuthContext userAndRolesAuthContext) {
        new ResolvedExecutionFileLoaderService(
                authContext: userAndRolesAuthContext,
                authorizingExecutionFileLoaderService: this,
                delegate: logFileStorageService
        )
    }
}
