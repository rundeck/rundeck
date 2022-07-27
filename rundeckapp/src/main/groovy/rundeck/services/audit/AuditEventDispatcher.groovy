package rundeck.services.audit

import com.dtolabs.rundeck.core.audit.ActionTypes
import com.dtolabs.rundeck.core.audit.AuditEvent
import com.dtolabs.rundeck.core.audit.ResourceTypes
import com.dtolabs.rundeck.plugins.audit.AuditEventListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory

public final class AuditEventDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(AuditEventDispatcher.class)

    private AuditEventDispatcher() {}

    /**
     * Dispatch an event to a listener.
     */
    static dispatchToListener(AuditEvent event, AuditEventListener listener) {

        try {

            // Call general callback
            listener.onEvent(event)

            // Call specific callbacks
            if (ResourceTypes.USER == event.resourceInfo.type) {
                switch (event.actionType) {
                    case ActionTypes.LOGIN_SUCCESS:
                        listener.onLoginSuccess(event)
                        break

                    case ActionTypes.LOGIN_FAILED:
                        listener.onLoginFailed(event)
                        break

                    case ActionTypes.LOGOUT:
                        listener.onLogout(event)
                        break
                }
            } else if (ResourceTypes.PROJECT == event.resourceInfo.type) {
                switch (event.actionType) {
                    case ActionTypes.VIEW:
                        listener.onProjectView(event)
                        break
                }
            } else if (ResourceTypes.JOB == event.resourceInfo.type) {
                switch (event.actionType) {
                    case ActionTypes.CREATE:
                        listener.onJobCreate(event)
                        break

                    case ActionTypes.UPDATE:
                        listener.onJobUpdate(event)
                        break

                    case ActionTypes.DELETE:
                        listener.onJobDelete(event)
                        break

                    case ActionTypes.RUN:
                        listener.onJobRun(event)
                        break

                }
            } else if (ResourceTypes.SYSTEM_ACL == event.resourceInfo.type ||
                ResourceTypes.PROJECT_ACL == event.resourceInfo.type) {
                switch (event.actionType) {
                    case ActionTypes.CREATE:
                        listener.onAclCreate(event)
                        break

                    case ActionTypes.UPDATE:
                        listener.onAclUpdate(event)
                        break

                    case ActionTypes.DELETE:
                        listener.onAclDelete(event)
                        break
                }
            }

        }
        catch (Exception e) {
            LOG.error("Error dispatching event to handler plugin: " + e.getMessage(), e)
        }
    }

}
