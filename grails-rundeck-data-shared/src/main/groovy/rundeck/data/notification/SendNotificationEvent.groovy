package rundeck.data.notification

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext

/**
 * Event trigger to send notifications for an execution
 */
class SendNotificationEvent {
    String executionUuid
    String jobUuid
    UserAndRolesAuthContext authContext
}
