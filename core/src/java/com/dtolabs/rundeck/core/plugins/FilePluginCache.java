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
* FilePluginCache.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/12/11 3:45 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.utils.PairImpl;
import com.dtolabs.rundeck.core.utils.cache.FileCache;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.*;

/**
 * FilePluginCache uses a filecache and a set of {@link PluginScanner}s to cache and create {@link ProviderLoader} instances
 * associated with files.
 * <p/>
 * The instances are returned for {@link ProviderIdent} instances.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class FilePluginCache implements PluginCache {
    static Logger log = Logger.getLogger(FilePluginCache.class.getName());

    /**
     * Cache item is a File, PluginDirScanner pair
     */
    static class cacheItem extends PairImpl<File, PluginScanner> {
        cacheItem(final File first, final PluginScanner second) {
            super(first, second);
        }
    }

    /**
     * Cache a file,pluginscanner for each ident
     */
    final private HashMap<ProviderIdent, cacheItem> cache = new HashMap<ProviderIdent, cacheItem>();
    final private HashMap<ProviderIdent, Long> expire = new HashMap<ProviderIdent, Long>();

    /**
     * the filecache of loaders associated with files
     */
    final private FileCache<ProviderLoader> filecache;
    /**
     * Scanners to use
     */
    final List<PluginScanner> scanners;

    FilePluginCache(final FileCache<ProviderLoader> filecache) {
        this.filecache = filecache;
        scanners = new ArrayList<PluginScanner>();
    }

    /**
     * Add a new scanner
     */
    public void addScanner(final PluginScanner scanner) {
        scanners.add(scanner);
    }

    /**
     * Remove the association with ident, and remove any filecache association as well.
     */
    private void remove(final ProviderIdent ident) {
        final cacheItem cacheItem = cache.get(ident);
        if (null != cacheItem) {
            filecache.remove(cacheItem.getFirst());
        }
        cache.remove(ident);
        expire.remove(ident);
    }

    /**
     * Get the loader for the provider
     *
     * @param ident provider ident
     *
     * @return loader for the provider
     */
    public synchronized ProviderLoader getLoaderForIdent(final ProviderIdent ident) {
        final cacheItem cacheItem = cache.get(ident);
        if (null == cacheItem) {
            log.debug("getLoaderForIdent!: " + ident);
            return rescanForItem(ident);
        }
        log.debug("getLoaderForIdent: " + ident);
        final File file = cacheItem.getFirst();
        if (!file.exists() || null == expire.get(ident) || file.lastModified() > expire.get(ident)) {
            remove(ident);
            log.debug("getLoaderForIdent(reload): " + ident + ": " + file);
            return rescanForItem(ident);
        } else {
            return loadFileProvider(cacheItem);
        }
    }

    /**
     * return the loader stored in filecache for the file and scanner
     */
    private ProviderLoader loadFileProvider(final cacheItem cached) {
        final File file = cached.getFirst();
        final PluginScanner second = cached.getSecond();
        log.debug("loadFileProvider(filecache): " + file);
        return filecache.get(file, second);
    }

    /**
     * Rescan for the ident and cache and return the loader
     */
    private synchronized ProviderLoader rescanForItem(final ProviderIdent ident) {
        log.debug("rescanForItem: " + ident);
        for (final PluginScanner scanner : scanners) {
            final File file = scanner.scanForFile(ident);
            if (null != file) {
                log.debug("file scanned:" + file);
                final cacheItem cacheItem = new cacheItem(file, scanner);
                cache.put(ident, cacheItem);
                expire.put(ident, file.lastModified());
                return loadFileProvider(cacheItem);
            }
        }
        return null;
    }
}
