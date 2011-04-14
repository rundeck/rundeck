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

import com.dtolabs.rundeck.core.utils.cache.FileCache;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
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

    protected DirPluginScanner(final File extdir, final FileCache<ProviderLoader> filecache) {
        this.extdir = extdir;
        this.filecache = filecache;
    }

    /**
     * Return true if the file is a valid plugin file for the scanner
     */
    public abstract boolean isValidPluginFile(final File file);

    /**
     * Return the file filter
     */
    public abstract FileFilter getFileFilter();

    public final File scanForFile(final ProviderIdent ident) {
        debug("scanForFile: " + ident);
        if (!extdir.exists() || !extdir.isDirectory()) {
            return null;
        }
        final File[] files = extdir.listFiles(getFileFilter());
        for (final File file : files) {
            if (isValidPluginFile(file)) {
                final ProviderLoader fileProviderLoader = filecache.get(file, this);
                final boolean loaderFor = fileProviderLoader.isLoaderFor(ident);
                debug("filecache result: " + fileProviderLoader + ", loaderForIdent: " + loaderFor);
                if (null != fileProviderLoader && loaderFor) {
                    return file;
                }
            }
        }
        return null;
    }

    private void debug(final String s) {
        if (log.isDebugEnabled()) {
            log.debug(s);
        }
    }
}
