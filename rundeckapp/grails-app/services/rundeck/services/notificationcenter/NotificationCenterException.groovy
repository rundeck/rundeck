package rundeck.services.notificationcenter

class NotificationCenterException extends Throwable{
    NotificationCenterException(String errorMessage, Throwable t) {
        super(errorMessage, t)
    }

    NotificationCenterException(String errorMessage) {
        super(errorMessage)
    }
}
