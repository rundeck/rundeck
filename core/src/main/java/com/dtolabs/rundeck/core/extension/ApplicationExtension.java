package com.dtolabs.rundeck.core.extension;

import java.util.Map;

public interface ApplicationExtension {
    /**
     * Name
     */
    String getName();

    /**
     * @return metadata about the extension
     */
    Map<String, String> getInfoMetadata();
}
