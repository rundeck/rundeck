package com.dtolabs.rundeck.plugins.audit;

public interface AuditEventListenerPlugin
    extends AuditEventListener {

  /**
   * Initialization method called after the plugin instance has been created.
   * Use this method to perform initialization procedures.
   */
  default void init() {
  }

}
