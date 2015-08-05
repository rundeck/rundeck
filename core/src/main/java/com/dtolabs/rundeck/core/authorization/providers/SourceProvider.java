package com.dtolabs.rundeck.core.authorization.providers;

import java.util.Iterator;

/**
 * Created by greg on 7/21/15.
 */
public interface SourceProvider {
    Iterator<CacheableYamlSource> getSourceIterator();
}
