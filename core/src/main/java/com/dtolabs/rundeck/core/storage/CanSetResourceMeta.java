package com.dtolabs.rundeck.core.storage;

import java.util.Date;
import java.util.Map;

/**
 * Mutable interface for metadata
 */
public interface CanSetResourceMeta {
    public void setMeta(Map<String,String> meta);
    public void setMeta(String key, String value);
    public void setContentType(String value);

    public void setContentLength(long length);

    public void setModificationTime(Date time);

    public void setCreationTime(Date time);
}
