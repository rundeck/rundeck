package com.dtolabs.rundeck.plugins.audit;

import com.dtolabs.rundeck.core.audit.AuditEvent;

/**
 * Interface for implementing an Audit event handler plugin.
 */
public interface AuditEventsHandlerPlugin {


  /**
   * Handles a successful authentication event.
   * @param event Event descriptor.
   */
  default void onLoginSuccessful(AuditEvent event){};

  /**
   * Handles an authentication failure event.
   * @param event Event descriptor.
   */
  default void onLoginFailure(AuditEvent event){};

  /**
   * Handles a successful session logout event.
   * @param event Event descriptor.
   */
  default void onLogoutSuccessful(AuditEvent event){};


  /**
   * Handles a Project access event.
   * @param event Event descriptor.
   */
  default void onProjectAccess(AuditEvent event){};


}
