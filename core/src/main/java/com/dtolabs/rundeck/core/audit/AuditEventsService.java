package com.dtolabs.rundeck.core.audit;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.execution.service.ExecutionServiceException;
import com.dtolabs.rundeck.core.execution.service.ProviderCreationException;
import com.dtolabs.rundeck.core.plugins.*;
import com.dtolabs.rundeck.core.plugins.configuration.DescribableServiceUtil;
import com.dtolabs.rundeck.core.plugins.configuration.Description;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.audit.AuditEventsHandler;
import com.dtolabs.rundeck.plugins.audit.SystemAuditEventsHandler;

import java.security.Principal;
import java.util.Date;
import java.util.List;

public class AuditEventsService
extends PluggableProviderRegistryService<AuditEventsHandler>
implements PluggableProviderService<AuditEventsHandler>,
    JavaClassProviderLoadable<AuditEventsHandler> {

  public static final String SERVICE_NAME = ServiceNameConstants.AuditEventsHandler;

  public AuditEventsService(Framework framework) {
    super(framework, true);
    registry.put(SystemAuditEventsHandler.SERVICE_PROVIDER_TYPE, SystemAuditEventsHandler.class);
  }



  @Override
  public String getName() {
    return SERVICE_NAME;
  }

  public static AuditEventsService getInstanceForFramework(Framework framework) {
    if (null == framework.getService(SERVICE_NAME)) {
      final AuditEventsService service = new AuditEventsService(framework);
      framework.setService(SERVICE_NAME, service);
      return service;
    }
    return (AuditEventsService) framework.getService(SERVICE_NAME);
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
        .forEach(auditEventsHandler -> {
          System.err.println("   !!! AEH: " + auditEventsHandler.toString());
          auditEventsHandler.onLoginSuccessful(new AuditEvent() {
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
    return AuditEventsHandler.class.isAssignableFrom(clazz) && hasValidProviderSignature(clazz);
  }

  @Override
  public <X extends AuditEventsHandler> AuditEventsHandler createProviderInstance(Class<X> clazz, String name) throws PluginException, ProviderCreationException {
    return createProviderInstanceFromType(clazz, name);
  }
}
