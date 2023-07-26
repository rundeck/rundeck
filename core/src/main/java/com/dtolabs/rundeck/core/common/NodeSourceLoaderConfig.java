package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.plugins.CloseableProvider;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import org.rundeck.app.spi.Services;

public interface NodeSourceLoaderConfig {

    CloseableProvider<ResourceModelSource> getCloseableProvider();

    default Services getServices(){
        return null;
    };
}
