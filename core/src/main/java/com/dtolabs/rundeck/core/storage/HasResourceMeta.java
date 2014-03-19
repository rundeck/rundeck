package com.dtolabs.rundeck.core.storage;

import java.util.Map;

/**
 * Reads metadata
 */
public interface HasResourceMeta {
    public Map<String,String> getResourceMeta();
}
