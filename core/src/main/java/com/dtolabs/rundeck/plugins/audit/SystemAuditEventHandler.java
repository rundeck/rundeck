package com.dtolabs.rundeck.plugins.audit;


import com.dtolabs.rundeck.core.audit.AuditEvent;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;

@Plugin(name = SystemAuditEventHandler.SERVICE_PROVIDER_TYPE, service = ServiceNameConstants.AuditEventHandler)
@PluginDescription(title = "System Audit", description = "System builtin auditing event handler")
public class SystemAuditEventHandler implements AuditEventHandler {

  public static final String    SERVICE_PROVIDER_TYPE = "system-audit";

  public SystemAuditEventHandler(Framework framework) {
    System.err.println("CORE CONSTRUCTOR");
  }

  @Override
  public void onLoginSuccessful(AuditEvent event) {
    System.err.println("LOGINYES: " + event.toString());
  }

  @Override
  public void onLoginFailure(AuditEvent event) {
    System.err.println("LOGINNO: " + event.toString());
  }


  @Override
  public void onProjectAccess(AuditEvent event) {
    System.err.println("PROJACCESS: " + event.toString());
  }
}
