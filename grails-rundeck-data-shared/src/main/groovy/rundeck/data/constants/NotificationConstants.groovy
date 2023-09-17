package rundeck.data.constants

class NotificationConstants {

    public static final String ONSUCCESS_TRIGGER_NAME = 'onsuccess'
    public static final String ONFAILURE_TRIGGER_NAME = 'onfailure'
    public static final String ONSTART_TRIGGER_NAME = 'onstart'
    public static final String ONAVGDURATION_TRIGGER_NAME = 'onavgduration'
    public static final String ONRETRYABLEFAILURE_TRIGGER_NAME = 'onretryablefailure'
    public static final Set<String> TRIGGER_NAMES = Collections.unmodifiableSet(
        new HashSet<String>(
            [
                ONSUCCESS_TRIGGER_NAME,
                ONFAILURE_TRIGGER_NAME,
                ONSTART_TRIGGER_NAME,
                ONAVGDURATION_TRIGGER_NAME,
                ONRETRYABLEFAILURE_TRIGGER_NAME
            ]
        )
    )

    public static final String EMAIL_NOTIFICATION_TYPE = 'email'
    public static final String WEBHOOK_NOTIFICATION_TYPE = 'url'
}