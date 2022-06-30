package org.rundeck.plugin.audit

import com.dtolabs.rundeck.core.audit.AuditEvent
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.audit.AuditEventListenerPlugin
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Plugin(
    name = PROVIDER_NAME,
    service = ServiceNameConstants.AuditEventListener)
@PluginDescription(
    title = "Audit Logging Plugin",
    description = "Writes audit events to the service log")
class AuditLoggerPlugin implements AuditEventListenerPlugin {

  public static final String PROVIDER_NAME = "audit-logger-plugin"
  private static final Logger LOGGER = LoggerFactory.getLogger(AuditLoggerPlugin.class)

  @Override
  void init() {
    LOGGER.debug("Audit events logging plugin initialized.")
  }

  /**
   * Log received events
   */
  void onEvent(AuditEvent event) {
    LOGGER.info("Audit Event: " + event)
  }
}


