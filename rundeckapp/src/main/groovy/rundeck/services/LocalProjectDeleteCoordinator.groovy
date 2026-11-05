package rundeck.services

import com.dtolabs.rundeck.core.common.ProjectDeleteCoordinator
import groovy.transform.CompileStatic

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Prevents duplicate deferred deletion work within one Rundeck process.
 */
@CompileStatic
class LocalProjectDeleteCoordinator implements ProjectDeleteCoordinator {
    private final ConcurrentMap<Long, String> claims = new ConcurrentHashMap<>()

    @Override
    String tryAcquire(final String projectName, final long projectId) {
        String token = UUID.randomUUID().toString()
        claims.putIfAbsent(projectId, token) == null ? token : null
    }

    @Override
    boolean isOwner(final String projectName, final long projectId, final String claimToken) {
        claimToken != null && claimToken == claims.get(projectId)
    }

    @Override
    void release(final String projectName, final long projectId, final String claimToken) {
        if (claimToken != null) {
            claims.remove(projectId, claimToken)
        }
    }
}
