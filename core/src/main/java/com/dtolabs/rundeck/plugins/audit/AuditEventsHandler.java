package com.dtolabs.rundeck.plugins.audit;

import com.dtolabs.rundeck.core.audit.AuditEvent;

/**
 * Interface for implementing an Audit event handler plugin.
 */
public interface AuditEventsHandler {


  default void onEvent(AuditEvent event) {}

  /**
   * Handles a successful authentication event.
   * @param event Event descriptor.
   */
  default void onLoginSuccessfulEvent(AuditEvent event){}

  /**
   * Handles an authentication failure event.
   * @param event Event descriptor.
   */
  default void onLoginFailureEvent(AuditEvent event){}

  /**
   * Handles a successful session logout event.
   * @param event Event descriptor.
   */
  default void onLogoutSuccessfulEvent(AuditEvent event){}


  /**
   * Handles a Project access event.
   * @param event Event descriptor.
   */
  default void onProjectAccessEvent(AuditEvent event){}


}
