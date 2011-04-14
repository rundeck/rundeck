/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* PluginFileCache.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/12/11 6:24 PM
* 
*/
package com.dtolabs.rundeck.core.utils.cache;

import java.io.File;
import java.util.*;

/**
 * FileCache stores items associated with a file and expires them if the file is removed or modified. The {@link
 * ItemCreator} interface is used to create cacheable items
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class FileCache<T extends Cacheable> {
    private HashMap<File, T> cache = new HashMap<File, T>();
    private HashMap<File, Long> expiry = new HashMap<File, Long>();


    /**
     * Remove entry for a file.
     */
    public synchronized void remove(final File file) {
        final T t = cache.get(file);
        expiry.remove(file);
        cache.remove(file);
        if (null != t && t instanceof Expireable) {
            final Expireable exp = (Expireable) t;
            exp.expire();
        }
    }

    /**
     * Get entry for a file, and use the creator if necessary to create it. The creator will be called if the cache is
     * out of date for the file. If the created item is equals to any existing cached item for the file then the old
     * item will not be replaced.  Otherwise, if the old item is {@link Expireable} then the {@link
     * Expireable#expire()} method will be called on it;
     *
     * @param file    the ifle
     * @param creator the item creator
     *
     * @return the item associated with the file, or null if none is found
     */
    public synchronized T get(final File file, final ItemCreator<T> creator) {
        if (!file.exists()) {
            remove(file);
            return null;
        }
        final long lastmod = file.lastModified();
        final Long cachetime = expiry.get(file);
        final T entry;
        final T orig = cache.get(file);
        if (null == cachetime || lastmod > cachetime) {
            entry = creator.createCacheItemForFile(file);
            if (null != entry) {
                if (null != orig && !entry.equals(orig)) {
                    remove(file);
                    cache.put(file, entry);
                } else if (null == orig) {
                    cache.put(file, entry);
                } else {
                    //noop, retain orig
                }
                expiry.put(file, lastmod);
            } else {
                remove(file);
            }
        } else {
            entry = cache.get(file);
        }
        return entry;
    }

    /**
     * Creates an item to store in the cache
     */
    public static interface ItemCreator<T> {
        /**
         * Return item to store for the file, or null to remove the association.
         */
        public T createCacheItemForFile(File file);
    }
}
