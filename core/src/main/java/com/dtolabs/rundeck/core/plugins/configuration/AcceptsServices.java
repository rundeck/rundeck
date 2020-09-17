package com.dtolabs.rundeck.core.plugins.configuration;

import org.rundeck.app.spi.Services;

public interface AcceptsServices {

    void setServices(Services services);
}
