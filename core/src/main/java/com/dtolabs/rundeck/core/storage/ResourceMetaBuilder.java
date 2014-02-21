package com.dtolabs.rundeck.core.storage;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 2/19/14 Time: 11:49 AM
 */
public class ResourceMetaBuilder implements CanSetResourceMeta, HasResourceMeta {
    Map<String, String> meta;

    public ResourceMetaBuilder(Map<String, String> meta) {
        if (null != meta) {
            this.meta = meta;
        }
    }

    public ResourceMetaBuilder() {
        this(new HashMap<String, String>());
    }

    @Override
    public void setMeta(Map<String, String> meta) {
        this.meta = meta;
    }

    @Override
    public void setMeta(String key, String value) {
        meta.put(key, value);
    }

    @Override
    public void setContentType(String value) {
        setMeta(ResourceUtil.RES_META_RUNDECK_CONTENT_TYPE, value);
    }

    @Override
    public void setContentLength(long length) {
        setMeta(ResourceUtil.RES_META_RUNDECK_CONTENT_LENGTH, Long.toString(length));
    }

    @Override
    public void setModificationTime(Date time) {
        setMeta(ResourceUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME, ResourceUtil.formatDate(time));
    }

    @Override
    public void setCreationTime(Date time) {
        setMeta(ResourceUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME, ResourceUtil.formatDate(time));
    }

    @Override
    public Map<String,String> getResourceMeta() {
        return meta;
    }
}
