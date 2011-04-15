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
* DirPluginScanner.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/13/11 6:29 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.utils.StringArrayUtil;
import com.dtolabs.rundeck.core.utils.cache.FileCache;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * DirPluginScanner will scan all files in a directory matching a filter for valid plugins.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
abstract class DirPluginScanner implements PluginScanner {
    static Logger log = Logger.getLogger(DirPluginScanner.class.getName());
    final File extdir;

    final FileCache<ProviderLoader> filecache;
    long lastScanAllCheckTime = -1;
    HashSet<String> scanned = new HashSet<String>();
    long scaninterval ;

    protected DirPluginScanner(final File extdir, final FileCache<ProviderLoader> filecache, final int rescanInterval) {
        this.extdir = extdir;
        this.filecache = filecache;
        this.scaninterval = rescanInterval;
    }

    /**
     * Return true if the file is a valid plugin file for the scanner
     */
    public abstract boolean isValidPluginFile(final File file);

    /**
     * Return the file filter
     */
    public abstract FileFilter getFileFilter();

    /**
     * scan for matching file for the provider def
     */
    public final File scanForFile(final ProviderIdent ident) throws PluginScannerException {
        debug("scanForFile: " + ident);
        if (!extdir.exists() || !extdir.isDirectory()) {
            return null;
        }
        final File[] files = extdir.listFiles(getFileFilter());
        if (shouldScanAll(files)) {
            return scanAll(ident, files);
        } else {
            return scanFor(ident, files);
        }
    }

    /**
     * Return true if the entry has expired
     */
    public boolean isExpired(final ProviderIdent ident, final File file) {
        return !file.exists() || !scanned.contains(memoFile(file));
    }

    /**
     * Return true if any file has been added/removed/modified, and the last full scan has not happened within a certain
     * interval
     */
    public boolean shouldRescan() {
        return shouldScanAll(extdir.listFiles(getFileFilter()));
    }

    /**
     * Return true if any file has been added/removed/modified, and the last full scan has not happened within a certain
     * interval
     */
    private boolean shouldScanAll(final File[] files) {
        if (lastScanAllCheckTime > 0 && lastScanAllCheckTime + scaninterval > System.currentTimeMillis()) {
            //wait until scaninterval has passed to scanall again
            log.debug("shouldScanAll: false, interval");
            return false;
        }
        if (scanned.size() != files.length) {
            log.debug("shouldScanAll: yes, count: " + scanned.size() + " vs " + files.length);
            scanned.clear();
            return true;
        }else{
            log.debug("(shouldScanAll: ...: " + scanned.size() + " vs " + files.length);
        }
        for (final File file : files) {
            final String s = memoFile(file);
            if (!scanned.contains(s)) {
                log.debug("shouldScanAll: yes, file: " + s);
                scanned.clear();
                return true;
            }
        }
        log.debug("shouldScanAll: false, no change");
        lastScanAllCheckTime = System.currentTimeMillis();
        return false;
    }

    private String memoFile(final File file) {
        return file.getName() + ":" + file.lastModified() + ":" + file.length();
    }

    /**
     * Return the first valid file found
     */
    private File scanFor(final ProviderIdent ident, final File[] files) throws PluginScannerException {
        for (final File file : files) {
            if (isValidPluginFile(file)) {
                if (test(ident, file)) {
                    return file;
                }
            }
        }
        return null;
    }

    /**
     * Test if a loader for this file matches the provider
     */
    private boolean test(final ProviderIdent ident, final File file) {
        final ProviderLoader fileProviderLoader = filecache.get(file, this);
        final boolean loaderFor = fileProviderLoader.isLoaderFor(ident);
        debug("filecache result: " + fileProviderLoader + ", loaderForIdent: " + loaderFor);
        return null != fileProviderLoader && loaderFor;
    }

    /**
     * Rescan all files in the directory
     */
    private File scanAll(final ProviderIdent ident, final File[] files) throws PluginScannerException {
        final List<File> candidates = new ArrayList<File>();
        scanned.clear();
        for (final File file : files) {
            if (isValidPluginFile(file)) {
                scanned.add(memoFile(file));
                if (test(ident, file)) {
                    candidates.add(file);
                }
            }
        }
        if (candidates.size() > 1) {
            scanned.clear();
            throw new PluginScannerException(
                "More than one plugin file matched: " + StringArrayUtil.asString(candidates.toArray(), ","),
                ident.getService(), ident.getProviderName());
        }
        lastScanAllCheckTime = System.currentTimeMillis();
        if (candidates.size() > 0) {
            return candidates.get(0);
        }
        return null;
    }

    private void debug(final String s) {
        if (log.isDebugEnabled()) {
            log.debug(s);
        }
    }
}
