package com.dtolabs.rundeck.core.audit;


import java.io.Serializable;
import java.security.Principal;
import java.util.Date;
import java.util.List;

/**
 * Describes an audit event.
 */
public interface AuditEvent extends Serializable {

  /**
   *
   * @return Event type.
   */
  AuditEventType getEventType();

  /**
   *
   * @return Timestamp of the event.
   */
  Date getTimestamp();

  /**
   * Gets the username
   * @return
   */
  String getUsername();

  /**
   * The roles associated with the user
   * @return
   */
  List<String> getUserRoles();

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
