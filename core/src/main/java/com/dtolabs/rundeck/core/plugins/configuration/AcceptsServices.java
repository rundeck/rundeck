package com.dtolabs.rundeck.core.plugins.configuration;

import org.rundeck.app.spi.Services;

interface AcceptsServices {

    public void setServices(Services services);
}
