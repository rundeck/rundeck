package com.dtolabs.rundeck.plugins.audit;


import com.dtolabs.rundeck.core.audit.AuditEvent;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;

@Plugin(name = SystemAuditEventsHandler.SERVICE_PROVIDER_TYPE, service = ServiceNameConstants.AuditEventsHandler)
@PluginDescription(title = "System Audit", description = "System builtin auditing event handler")
public class SystemAuditEventsHandler implements AuditEventsHandler {

  public static final String    SERVICE_PROVIDER_TYPE = "system-audit";

  public SystemAuditEventsHandler(Framework framework) {
    System.err.println("CORE CONSTRUCTOR");
  }

  @Override
  public void onLoginSuccessful(AuditEvent event) {
    System.err.println("SYSPL: " + event.toString());
  }

  @Override
  public void onLoginFailure(AuditEvent event) {
    System.err.println("SYSPL: " + event.toString());
  }


  @Override
  public void onProjectAccess(AuditEvent event) {
    System.err.println("SYSPL: " + event.toString());
  }
}
