/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.dtolabs.rundeck.core.utils.cache.FileCache.memoize;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * DirPluginScanner will scan all files in a directory matching a filter for valid plugins.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public abstract class DirPluginScanner implements PluginScanner, PluginDirChangeEventListener {
    static Logger log = LoggerFactory.getLogger(DirPluginScanner.class.getName());
    final  File   extdir;

    final FileCache<ProviderLoader> filecache;
    private HashSet<FileCache.MemoFile> scannedFiles = new HashSet<>();
    private HashMap<FileCache.MemoFile,Boolean> validity = new HashMap<>();

    private final AtomicBoolean       allowProviderRescan       = new AtomicBoolean(true);
    private final AtomicBoolean       allowFileRescan           = new AtomicBoolean(true);
    final         List<ProviderIdent> providerIdentList         = new ArrayList<ProviderIdent>();
    private       File[] cachedScannedFileList                  = new File[]{};

    protected DirPluginScanner(final PluginDirProvider pluginDirProvider, final FileCache<ProviderLoader> filecache) {
        if(pluginDirProvider != null) {
            this.extdir = pluginDirProvider.getPluginDir();
            pluginDirProvider.registerDirChangeEventListener(this);
            updateFileListCache();
        } else {
            this.extdir = null;
        }
        this.filecache = filecache;
    }

    @Override
    public void onDirChangeEvent(final PluginDirChangeEvent event) {
        allowFileRescan.set(true);
        allowProviderRescan.set(true);
    }

    private void updateFileListCache() {
        if (extdir == null || !extdir.exists() || !extdir.isDirectory()) {
            return;
        }
        cachedScannedFileList = extdir.listFiles(getFileFilter());
    }

    /**
     * Return true if the file is a valid plugin file for the scanner
     */
    public abstract boolean isValidPluginFile(final File file);

    boolean cachedFileValidity(final File file) {
        final FileCache.MemoFile memo = memoize(file);
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
    File resolveProviderConflict(final Collection<FileCache.MemoFile> matched){
        final HashMap<File, VersionCompare> versions = new HashMap<File, VersionCompare>();
        final ArrayList<File> toCompare = new ArrayList<File>();
        for (final FileCache.MemoFile file : matched) {
            final String vers = getVersionForFile(file.getFile());
            if (null != vers) {
                versions.put(file.getFile(), VersionCompare.forString(vers));
                toCompare.add(file.getFile());
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
        if(allowFileRescan.get()) {
            updateFileListCache();
            allowFileRescan.set(false);
        }
        return scanFor(ident, cachedScannedFileList);
    }

    public List<ProviderIdent> listProviders() {
        if(allowProviderRescan.get()) {
            final HashSet<ProviderIdent> providerIdentsHash = new HashSet<ProviderIdent>();
            providerIdentList.clear();
            updateFileListCache();
            for (final File file : cachedScannedFileList) {
                if (cachedFileValidity(file)) {
                    providerIdentsHash.addAll(listProviders(file));
                }
            }
            providerIdentList.addAll(providerIdentsHash);
            allowProviderRescan.set(false);
        }
        return providerIdentList;
    }


    /**
     * Return true if the entry has expired
     */
    public boolean isExpired(final ProviderIdent ident, final File file) {
        return !file.exists() || !scannedFiles.contains(memoize(file));
    }

    private void clearMemos(Set<FileCache.MemoFile> memos) {
        scannedFiles.removeAll(memos);
        for (FileCache.MemoFile memo : memos) {
            validity.remove(memo);
            filecache.remove(memo.getFile());
        }
    }


    /**
     * Return the first valid file found
     */
    private File scanFor(final ProviderIdent ident, File[] files) throws PluginScannerException {
        final List<FileCache.MemoFile> candidates = new ArrayList<>();
        HashSet<FileCache.MemoFile> prescanned = new HashSet<>(scannedFiles);
        HashSet<FileCache.MemoFile> newscanned = new HashSet<>();
        for (final File file : files) {
            FileCache.MemoFile memo = memoize(file);
            if (cachedFileValidity(file)) {
                newscanned.add(memo);
                if (test(ident, file)) {
                    candidates.add(memo);
                }
            }
            prescanned.remove(memo);
        }
        clearMemos(prescanned);
        scannedFiles = newscanned;

        if (candidates.size() == 1) {
            return candidates.get(0).getFile();
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
        try {
            final ProviderLoader fileProviderLoader = filecache.get(file, this);
            if (fileProviderLoader == null) {
                return false;
            }
            final boolean loaderFor = fileProviderLoader.isLoaderFor(ident);
            return loaderFor;
        } catch (Exception e) {
            // Plugin is broken or error occurred (e.g., class not found, I/O error) - log and skip it
            log.error("Error testing plugin file {}: {}", file.getAbsolutePath(), e.getMessage(), e);
            return false;
        }
    }
    private List<ProviderIdent> listProviders(final File file){
        final ProviderLoader fileProviderLoader = filecache.get(file, this);
        return fileProviderLoader.listProviders();
    }


    private void debug(final String s) {
        if (log.isDebugEnabled()) {
            log.debug(s);
        }
    }
}
