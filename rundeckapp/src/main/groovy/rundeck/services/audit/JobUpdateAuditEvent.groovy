package rundeck.services.audit

class JobUpdateAuditEvent {
    String jobUuid
    String project
    String fullName
    boolean isnew
}
