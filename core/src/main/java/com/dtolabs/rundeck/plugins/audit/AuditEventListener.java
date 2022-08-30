package com.dtolabs.rundeck.plugins.audit;

import com.dtolabs.rundeck.core.audit.AuditEvent;

/**
 * Interface for implementing an Audit event listener plugins.
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
  
  /** Called when a job is created */
  default void onJobCreate(AuditEvent event) {}

  /** Called when a job is updated */
  default void onJobUpdate(AuditEvent event) {}

  /** Called when a job is deleted */  
  default void onJobDelete(AuditEvent event) {}

  /** Called when a job is run */  
  default void onJobRun(AuditEvent event) {}

  /** Called when an acl is created */  
  default void onAclCreate(AuditEvent event) {}
  /** Called when an acl is updated */  
  default void onAclUpdate(AuditEvent event) {}
  /** Called when an acl is deleted */
  default void onAclDelete(AuditEvent event) {}

}
