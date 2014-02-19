package com.dtolabs.rundeck.core.resourcetree;

import java.util.Date;
import java.util.Map;

/**
 * $INTERFACE is ... User: greg Date: 2/19/14 Time: 11:48 AM
 */
public interface CanSetResourceMeta {
    public void setMeta(Map<String,String> meta);
    public void setMeta(String key, String value);
    public void setContentType(String value);

    public void setContentLength(long length);

    public void setModificationTime(Date time);

    public void setCreationTime(Date time);
}
