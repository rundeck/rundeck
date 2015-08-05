package com.dtolabs.rundeck.core.authorization.providers;

import java.util.Date;

/**
 * Created by greg on 7/17/15.
 */
public interface CacheableYamlSource extends YamlSource {
    public boolean isValid();
    public Date getLastModified();
}
