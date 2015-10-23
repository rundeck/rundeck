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
import java.util.*;

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
    HashMap<String,Boolean> validity = new HashMap<String, Boolean>();
    long scanintervalMs;

    protected DirPluginScanner(final File extdir, final FileCache<ProviderLoader> filecache, final long rescanIntervalMs) {
        this.extdir = extdir;
        this.filecache = filecache;
        this.scanintervalMs = rescanIntervalMs;
    }

    /**
     * Return true if the file is a valid plugin file for the scanner
     */
    public abstract boolean isValidPluginFile(final File file);

    boolean cachedFileValidity(final File file) {
        final String memo = memoFile(file);
        if (!validity.containsKey(memo)) {
            validity.put(memo, isValidPluginFile(file));
        }
        return validity.get(memo);
    }

    /**
     * Return the file filter
     */
    public abstract FileFilter getFileFilter();

    /**
     * Return a single file that should be used among all te files matching a single provider identity, or null if
     * the conflict cannot be resolved.
     */
    File resolveProviderConflict(final Collection<File> matched){
        final HashMap<File, VersionCompare> versions = new HashMap<File, VersionCompare>();
        final ArrayList<File> toCompare = new ArrayList<File>();
        for (final File file : matched) {
            final String vers = getVersionForFile(file);
            if (null != vers) {
                versions.put(file, VersionCompare.forString(vers));
                toCompare.add(file);
            }
        }
        //currently resolve via filename
        final Comparator<File> c = new VersionCompare.fileComparator(versions);
        final List<File> sorted = new ArrayList<File>(toCompare);
        Collections.sort(sorted, c);
        if (sorted.size() > 0) {
            return sorted.get(sorted.size() - 1);
        }
        return null;
    }

    /**
     * Return the version string for the plugin file, or null
     */
    protected abstract String getVersionForFile(File file);

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

    public List<ProviderIdent> listProviders() {
        try {
            doScanAll();
        } catch (PluginScannerException e) {
            //ignore conflict
        }
        final HashSet<ProviderIdent> providerIdentsHash = new HashSet<ProviderIdent>();
        final List<ProviderIdent> providerIdents = new ArrayList<ProviderIdent>();
        if(null!=extdir && extdir.isDirectory() ){
            final File[] files = extdir.listFiles(getFileFilter());
            if(null!=files){
                for (final File file : files) {
                    if (cachedFileValidity(file)) {
                        providerIdentsHash.addAll(listProviders(file));
                    }
                }
            }
        }
        providerIdents.addAll(providerIdentsHash);
        return providerIdents;
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
     * scan all found files to cache them
     * @throws PluginScannerException
     */
    public void doScanAll() throws PluginScannerException {
        File[] files = extdir.listFiles(getFileFilter());
        if(null==files) {
            files = new File[0];
        }
        if(shouldScanAll(files)) {
            log.debug("shouldScanAll true: doScanAll");
            scanAll(null, files);
        }
    }

    /**
     * Return true if any file has been added/removed/modified, and the last full scan has not happened within a certain
     * interval
     */
    private boolean shouldScanAll(final File[] files) {
        if (lastScanAllCheckTime > 0 && lastScanAllCheckTime + scanintervalMs > System.currentTimeMillis()) {
            //wait until scaninterval has passed to scanall again
            log.debug("shouldScanAll: false, interval");
            return false;
        }
        if (scanned.size() != files.length) {
            log.debug("shouldScanAll: yes, count: " + scanned.size() + " vs " + files.length);
            clearCache(files);
            return true;
        }else{
            log.debug("(shouldScanAll: ...: " + scanned.size() + " vs " + files.length);
        }
        for (final File file : files) {
            final String s = memoFile(file);
            final boolean validPluginFile = cachedFileValidity(file);
            if (validPluginFile && !scanned.contains(s)) {
                log.debug("shouldScanAll: yes, file: " + s);
                clearCache(files);
                return true;
            }else if(!validPluginFile && scanned.contains(s)){
                log.debug("shouldScanAll: yes, file: " + s);
                clearCache(files);
                return true;
            }
        }
        log.debug("shouldScanAll: false, no change");
        lastScanAllCheckTime = System.currentTimeMillis();
        return false;
    }

    private void clearCache(File[] files) {
        scanned.clear();
        for(File file:files) {
            filecache.remove(file);
        }
//        validity.clear();
    }

    private String memoFile(final File file) {
        return file.getName() + ":" + file.lastModified() + ":" + file.length();
    }

    /**
     * Return the first valid file found
     */
    private File scanFor(final ProviderIdent ident, final File[] files) throws PluginScannerException {
        final List<File> candidates = new ArrayList<File>();
        for (final File file : files) {
            if (cachedFileValidity(file)) {
                if (test(ident, file)) {
                    candidates.add(file);
                }
            }
        }
        if (candidates.size() == 1) {
            return candidates.get(0);
        }
        if (candidates.size() > 1) {
            final File resolved = resolveProviderConflict(candidates);
            if(null==resolved) {
                log.warn(
                        "More than one plugin file matched: " + StringArrayUtil.asString(candidates.toArray(), ",")
                        +": "+ ident
                );
            }
            else {
                return resolved;
            }
        }
        return null;
    }

    /**
     * Test if a loader for this file matches the provider
     */
    private boolean test(final ProviderIdent ident, final File file) {
        final ProviderLoader fileProviderLoader = filecache.get(file, this);
        final boolean loaderFor = null != fileProviderLoader && fileProviderLoader.isLoaderFor(ident);
        debug("filecache result: " + fileProviderLoader + ", loaderForIdent: " + loaderFor);
        return null != fileProviderLoader && loaderFor;
    }
    private List<ProviderIdent> listProviders(final File file){
        final ProviderLoader fileProviderLoader = filecache.get(file, this);
        return fileProviderLoader.listProviders();
    }

    /**
     * Rescan all files in the directory
     * @param ident ident to scan for, or null
     * @param files files
     * @return file matching provider ident if specified
     */
    private File scanAll(final ProviderIdent ident, final File[] files) throws PluginScannerException {
        final List<File> candidates = new ArrayList<File>();
        clearCache(files);
        for (final File file : files) {
            if (cachedFileValidity(file)) {
                scanned.add(memoFile(file));
                if (null!=ident && test(ident, file)) {
                    candidates.add(file);
                }
            }
        }
        if (null!=ident && candidates.size() > 1) {
            clearCache(files);
            final File resolved = resolveProviderConflict(candidates);
            if(null==resolved){
                throw new PluginScannerException(
                    "More than one plugin file matched: " + StringArrayUtil.asString(candidates.toArray(), ","),
                    ident.getService(), ident.getProviderName());
            }
            else {
                candidates.clear();
                candidates.add(resolved);
            }
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
