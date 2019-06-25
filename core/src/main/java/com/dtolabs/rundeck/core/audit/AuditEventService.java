package com.dtolabs.rundeck.core.audit;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.audit.AuditEventHandler;
import com.dtolabs.rundeck.plugins.audit.SystemAuditEventHandler;

import java.security.Principal;
import java.util.Date;
import java.util.List;

public class AuditEventService
extends PluggableProviderRegistryService<AuditEventHandler>
implements PluggableProviderService<AuditEventHandler>,
    JavaClassProviderLoadable<AuditEventHandler> {

  public static final String SERVICE_NAME = ServiceNameConstants.AuditEventHandler;

  public AuditEventService(Framework framework) {
    super(framework, true);
    registry.put(SystemAuditEventHandler.SERVICE_PROVIDER_TYPE, SystemAuditEventHandler.class);
  }

  @Override
  public String getName() {
    return SERVICE_NAME;
  }

  public static AuditEventService getInstanceForFramework(Framework framework) {
    if (null == framework.getService(SERVICE_NAME)) {
      final AuditEventService service = new AuditEventService(framework);
      framework.setService(SERVICE_NAME, service);
      return service;
    }
    return (AuditEventService) framework.getService(SERVICE_NAME);
  }


  public void lol() {

    System.err.println("!!! LOLING");
    listProviders().stream()
        .peek(p -> System.err.println("  !!! " + p.toString() ))
        .map(providerIdent -> {
          try {
            return providerOfType(providerIdent.getProviderName());
          } catch (ExecutionServiceException e) {
            throw new RuntimeException(e.getMessage(), e);
          }
        })
        .forEach(auditEventHandler -> {
          System.err.println("   !!! AEH: " + auditEventHandler.toString());
          auditEventHandler.onLoginSuccessful(new AuditEvent() {
            @Override
            public Date getTimestamp() {
              return null;
            }

            @Override
            public Principal getPrincipal() {
              return null;
            }

            @Override
            public AuditEventType getEventType() {
              return null;
            }

            @Override
            public String getProjectName() {
              return null;
            }
          });
        });


  }


  @Override
  public List<ProviderIdent> listDescribableProviders() {
    return DescribableServiceUtil.listDescribableProviders(this);
  }

  @Override
  public List<Description> listDescriptions() {
    return DescribableServiceUtil.listDescriptions(this, false);
  }

  @Override
  public boolean isValidProviderClass(Class clazz) {
    return AuditEventHandler.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
  }

  @Override
  public <X extends AuditEventHandler> AuditEventHandler createProviderInstance(Class<X> clazz, String name) throws PluginException, ProviderCreationException {
    return createProviderInstanceFromType(clazz, name);
  }
}
