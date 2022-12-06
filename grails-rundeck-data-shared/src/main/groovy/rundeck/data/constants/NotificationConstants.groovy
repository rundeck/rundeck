package rundeck.data.constants

class NotificationConstants {

    public static final String NOTIFY_ONSUCCESS_EMAIL = 'notifyOnsuccessEmail'
    public static final String NOTIFY_ONFAILURE_EMAIL = 'notifyOnfailureEmail'
    public static final String NOTIFY_ONSTART_EMAIL = 'notifyOnstartEmail'
    public static final String NOTIFY_START_RECIPIENTS = 'notifyStartRecipients'
    public static final String NOTIFY_START_SUBJECT = 'notifyStartSubject'
    public static final String NOTIFY_ONSUCCESS_URL = 'notifyOnsuccessUrl'
    public static final String NOTIFY_SUCCESS_URL = 'notifySuccessUrl'
    public static final String NOTIFY_SUCCESS_URL_FORMAT = 'notifySuccessUrlFormat'
    public static final String NOTIFY_FAILURE_RECIPIENTS = 'notifyFailureRecipients'
    public static final String NOTIFY_FAILURE_SUBJECT= 'notifyFailureSubject'
    public static final String NOTIFY_FAILURE_ATTACH= 'notifyFailureAttach'
    public static final String NOTIFY_FAILURE_ATTACH_TYPE= 'notifyFailureAttachType'
    public static final String NOTIFY_SUCCESS_RECIPIENTS = 'notifySuccessRecipients'
    public static final String NOTIFY_SUCCESS_SUBJECT= 'notifySuccessSubject'
    public static final String NOTIFY_SUCCESS_ATTACH= 'notifySuccessAttach'
    public static final String NOTIFY_SUCCESS_ATTACH_TYPE= 'notifySuccessAttachType'
    public static final String NOTIFY_FAILURE_URL = 'notifyFailureUrl'
    public static final String NOTIFY_FAILURE_URL_FORMAT = 'notifyFailureUrlFormat'
    public static final String NOTIFY_ONFAILURE_URL = 'notifyOnfailureUrl'
    public static final String NOTIFY_ONSTART_URL = 'notifyOnstartUrl'
    public static final String NOTIFY_START_URL = 'notifyStartUrl'
    public static final String NOTIFY_START_URL_FORMAT = 'notifyStartUrlFormat'
    public static final String ONSUCCESS_TRIGGER_NAME = 'onsuccess'
    public static final String ONFAILURE_TRIGGER_NAME = 'onfailure'
    public static final String ONSTART_TRIGGER_NAME = 'onstart'
    public static final String OVERAVGDURATION_TRIGGER_NAME = 'onavgduration'
    public static final String ONRETRYABLEFAILURE_TRIGGER_NAME = 'onretryablefailure'
    public static final String NOTIFY_OVERAVGDURATION_EMAIL = 'notifyAvgDurationEmail'
    public static final String NOTIFY_OVERAVGDURATION_URL = 'notifyAvgDurationUrl'
    public static final String NOTIFY_OVERAVGDURATION_URL_FORMAT = 'notifyAvgDurationUrlFormat'
    public static final String NOTIFY_ONOVERAVGDURATION_URL = 'notifyOnAvgDurationUrl'
    public static final String NOTIFY_OVERAVGDURATION_RECIPIENTS = 'notifyAvgDurationRecipients'
    public static final String NOTIFY_OVERAVGDURATION_SUBJECT = 'notifyAvgDurationSubject'
    public static final String NOTIFY_ONRETRYABLEFAILURE_URL = 'notifyOnRetryableFailureUrl'
    public static final String NOTIFY_ONRETRYABLEFAILURE_EMAIL = 'notifyOnRetryableFailureEmail'
    public static final String NOTIFY_RETRYABLEFAILURE_URL = 'notifyRetryableFailureUrl'
    public static final String NOTIFY_RETRYABLEFAILURE_URL_FORMAT = 'notifyRetryableFailureUrlFormat'
    public static final String NOTIFY_RETRYABLEFAILURE_RECIPIENTS = 'notifyRetryableFailureRecipients'
    public static final String NOTIFY_RETRYABLEFAILURE_SUBJECT = 'notifyRetryableFailureSubject'
    public static final String NOTIFY_RETRYABLEFAILURE_ATTACH= 'notifyRetryableFailureAttach'
    public static final String NOTIFY_RETRYABLEFAILURE_ATTACH_TYPE= 'notifyRetryableFailureType'

    public static final String EMAIL_NOTIFICATION_TYPE = 'email'
    public static final String WEBHOOK_NOTIFICATION_TYPE = 'url'
    public static final ArrayList<String> NOTIFICATION_ENABLE_FIELD_NAMES = [
            NOTIFY_ONFAILURE_URL,
            NOTIFY_ONFAILURE_EMAIL,
            NOTIFY_ONSUCCESS_EMAIL,
            NOTIFY_ONSUCCESS_URL,
            NOTIFY_ONSTART_EMAIL,
            NOTIFY_ONSTART_URL,
            NOTIFY_OVERAVGDURATION_EMAIL,
            NOTIFY_ONOVERAVGDURATION_URL,
            NOTIFY_ONRETRYABLEFAILURE_EMAIL,
            NOTIFY_ONRETRYABLEFAILURE_URL
    ]

    static final Map NOTIFICATION_FIELD_NAMES= [
            ONSUCCESS_TRIGGER_NAME: NOTIFY_SUCCESS_RECIPIENTS,
            ONFAILURE_TRIGGER_NAME: NOTIFY_FAILURE_RECIPIENTS,
            ONSTART_TRIGGER_NAME: NOTIFY_START_RECIPIENTS,
            OVERAVGDURATION_TRIGGER_NAME: NOTIFY_OVERAVGDURATION_RECIPIENTS,
            ONRETRYABLEFAILURE_TRIGGER_NAME: NOTIFY_RETRYABLEFAILURE_RECIPIENTS
    ]
    static final Map NOTIFICATION_FIELD_ATTACHED_NAMES=[
            ONSUCCESS_TRIGGER_NAME: NOTIFY_SUCCESS_ATTACH,
            ONFAILURE_TRIGGER_NAME: NOTIFY_FAILURE_ATTACH,
            ONRETRYABLEFAILURE_TRIGGER_NAME: NOTIFY_RETRYABLEFAILURE_ATTACH
    ]

    static final Map NOTIFICATION_FIELD_NAMES_URL=[
            ONSUCCESS_TRIGGER_NAME: NOTIFY_SUCCESS_URL,
            ONFAILURE_TRIGGER_NAME: NOTIFY_FAILURE_URL,
            ONSTART_TRIGGER_NAME: NOTIFY_START_URL,
            OVERAVGDURATION_TRIGGER_NAME: NOTIFY_OVERAVGDURATION_URL,
            ONRETRYABLEFAILURE_TRIGGER_NAME: NOTIFY_RETRYABLEFAILURE_URL
    ]

}