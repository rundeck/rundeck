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
* PluginCache.java
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
 * PluginCache is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class PluginCache {
    static Logger log = Logger.getLogger(PluginCache.class.getName());

    static class cacheItem extends PairImpl<File, PluginDirScanner> {
        cacheItem(final File first, final PluginDirScanner second) {
            super(first, second);
        }
    }

    final private HashMap<ProviderIdent, cacheItem> cache = new HashMap<ProviderIdent, cacheItem>();
    final private HashMap<ProviderIdent, Long> expire = new HashMap<ProviderIdent, Long>();

    final private FileCache<FileProviderLoader> filecache;
    final List<PluginDirScanner> scanners;

    PluginCache(final FileCache<FileProviderLoader> filecache) {
        this.filecache = filecache;
        scanners = new ArrayList<PluginDirScanner>();
    }

    public void addScanner(final PluginDirScanner scanner) {
        scanners.add(scanner);
    }

    private void remove(final ProviderIdent ident) {
        final cacheItem cacheItem = cache.get(ident);
        if(null!=cacheItem) {
            filecache.remove(cacheItem.getFirst());
        }
        cache.remove(ident);
        expire.remove(ident);
    }

    /**
     * Get the loader for the provider ident
     *
     * @param ident provider ident
     *
     * @return loader for the provider
     */
    public synchronized FileProviderLoader getLoaderForIdent(final ProviderIdent ident) {
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
     * return the loader for the ident
     */
    private FileProviderLoader loadFileProvider(final cacheItem cached) {
        final File file = cached.getFirst();
        final PluginDirScanner second = cached.getSecond();
        log.debug("loadFileProvider(filecache): " + file);
        return filecache.get(file, second);
    }

    /**
     * Rescan for the ident and cache and return the loader
     */
    private synchronized FileProviderLoader rescanForItem(final ProviderIdent ident) {
        log.debug("rescanForItem: " + ident);
        for (final PluginDirScanner scanner : scanners) {
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
