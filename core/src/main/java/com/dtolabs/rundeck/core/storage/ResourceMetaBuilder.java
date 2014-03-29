package com.dtolabs.rundeck.core.storage;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Mutable set of rundeck resource metadata.
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
        setMeta(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE, value);
    }

    public String getContentType() {
        return getResourceMeta().get(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE);
    }

    @Override
    public void setContentLength(long length) {
        setMeta(StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH, Long.toString(length));
    }

    public long getContentLength() {
        return Long.parseLong(getResourceMeta().get(StorageUtil.RES_META_RUNDECK_CONTENT_LENGTH));
    }

    @Override
    public void setModificationTime(Date time) {
        setMeta(StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME, StorageUtil.formatDate(time));
    }

    public Date getModificationTime() {
        String s = getResourceMeta().get(StorageUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME);
        if (null != s) {
            return StorageUtil.parseDate(s, null);
        }
        return null;
    }

    @Override
    public void setCreationTime(Date time) {
        setMeta(StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME, StorageUtil.formatDate(time));
    }

    public Date getCreationTime() {
        String s = getResourceMeta().get(StorageUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME);
        if (null != s) {
            return StorageUtil.parseDate(s, null);
        }
        return null;
    }

    @Override
    public Map<String, String> getResourceMeta() {
        return meta;
    }
}
