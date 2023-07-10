package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.plugins.CloseableProvider;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;

public interface NodeSourceLoader {

    NodeSourceLoaderConfig getSourceForConfiguration(String project, SourceDefinition sourceDefinition);
}
