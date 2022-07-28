package com.dtolabs.rundeck.plugins.audit;

import com.dtolabs.rundeck.core.audit.AuditEvent;

/**
 * Interface for implementing an Audit event listener plugin.
 *
 * @author alberto
 */
public interface AuditEventListener {

  /**
   * Called when any kind of event is fired by the system.
   * This method is called always and before any other method of this listener.
   *
   * @param event The event descriptor fired by the system.
   */
  default void onEvent(AuditEvent event) {}


  /* quick listeners */

  /**
   * Called when a user logins successfully.
   *
   * @param event Event descriptor.
   */
  default void onLoginSuccess(AuditEvent event){}

  /**
   * Called on an authentication failure event.
   * @param event Event descriptor.
   */
  default void onLoginFailed(AuditEvent event){}

  /**
   * Called when a user logs out.
   * @param event Event descriptor.
   */
  default void onLogout(AuditEvent event){}


  /**
   * Called when the project homepage is accessed.
   *
   * @param event Event descriptor.
   */
  default void onProjectView(AuditEvent event){}


}
