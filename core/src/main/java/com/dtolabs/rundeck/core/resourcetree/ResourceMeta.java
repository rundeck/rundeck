package com.dtolabs.rundeck.core.resourcetree;

import us.vario.greg.lct.model.ContentMeta;

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
