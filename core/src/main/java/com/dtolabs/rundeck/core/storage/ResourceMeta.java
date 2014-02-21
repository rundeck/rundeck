package com.dtolabs.rundeck.core.storage;

import org.rundeck.storage.api.ContentMeta;

import java.util.Date;

/**
 * Extends ContentMeta to add metadata about a Rundeck resource.
 */
public interface ResourceMeta extends ContentMeta {
    public String getContentType();
    public long getContentLength();
    public Date getModificationTime();
    public Date getCreationTime();
}
