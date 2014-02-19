package com.dtolabs.rundeck.core.resourcetree;

import java.util.Date;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 2/19/14 Time: 11:38 AM
 */
abstract class BaseResource implements ResourceMeta {
    Map<String,String> meta;

    protected BaseResource(Map<String, String> meta) {
        this.meta = meta;
    }

    @Override
    public Map<String, String> getMeta() {
        return meta;
    }

    @Override
    public String getContentType() {
        return meta.get(ResourceUtil.RES_META_RUNDECK_CONTENT_TYPE);
    }

    @Override
    public long getContentLength() {
        return ResourceUtil.parseLong(meta.get(ResourceUtil.RES_META_RUNDECK_CONTENT_LENGTH), -1);
    }

    @Override
    public Date getModificationTime() {
        return ResourceUtil.parseDate(meta.get(ResourceUtil.RES_META_RUNDECK_CONTENT_MODIFY_TIME), null);
    }

    @Override
    public Date getCreationTime() {
        return ResourceUtil.parseDate(meta.get(ResourceUtil.RES_META_RUNDECK_CONTENT_CREATION_TIME), null);
    }
}
