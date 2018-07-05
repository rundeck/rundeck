package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.core.plugins.configuration.Describable;

import java.util.Properties;

@Plugin(name = LocalResourceModelSourceFactory.SERVICE_PROVIDER_TYPE, service = "ResourceModelSource")
public class LocalResourceModelSourceFactory implements ResourceModelSourceFactory, Describable {
    public static final String    SERVICE_PROVIDER_TYPE = "local";
    private             Framework framework;

    public LocalResourceModelSourceFactory(final Framework framework) {
        this.framework = framework;
    }

    public ResourceModelSource createResourceModelSource(final Properties configuration) throws ConfigurationException {
        final LocalResourceModelSource fileResourceModelSource = new LocalResourceModelSource(framework);
        fileResourceModelSource.configure(configuration);
        return fileResourceModelSource;
    }

    public com.dtolabs.rundeck.core.plugins.configuration.Description getDescription() {
        return LocalResourceModelSource.createDescription();
    }
}
