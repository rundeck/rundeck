package com.dtolabs.rundeck.core.audit;


import java.io.Serializable;
import java.security.Principal;
import java.util.Date;

/**
 * Describes an audit event.
 */
public interface AuditEvent extends Serializable {

  /**
   *
   * @return Timestamp of the event.
   */
  Date getTimestamp();

  /**
   * Get the user principal.
   * @return
   */
  Principal getPrincipal();

  /**
   *
   * @return Event type.
   */
  AuditEventType getEventType();

  /**
   *
   * @return Name of the project that generated the event. Null if does not apply.
   */
  String getProjectName();


  /**
   * Audit event types
   */
  enum AuditEventType {
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    PROJECT_ACCESS,
  }

}
