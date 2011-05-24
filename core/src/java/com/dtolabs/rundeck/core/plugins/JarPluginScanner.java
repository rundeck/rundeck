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
* JarPluginDirScanner.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/12/11 6:19 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.utils.cache.FileCache;
import org.apache.log4j.Logger;

import java.io.*;

/**
 * JarPluginScanner scans for java Jar plugins in the extensions dir.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class JarPluginScanner extends DirPluginScanner {
    static Logger log = Logger.getLogger(JarPluginScanner.class.getName());
    public static final FileFilter FILENAME_FILTER = new FileFilter() {
        public boolean accept(final File file) {
            return file.isFile() && file.getName().endsWith(".jar");
        }
    };
    final File cachedir;

    JarPluginScanner(final File extdir, final File cachedir, final FileCache<ProviderLoader> filecache, final int rescanInterval) {
        super(extdir, filecache, rescanInterval);
        this.cachedir = cachedir;
    }

    public boolean isValidPluginFile(final File file) {
        return JarPluginProviderLoader.isValidJarPlugin(file);
    }


    public FileFilter getFileFilter() {
        return FILENAME_FILTER;
    }


    public ProviderLoader createCacheItemForFile(final File file) {
        return createLoader(file);
    }

    public ProviderLoader createLoader(final File file) {
        if (log.isDebugEnabled()) {
            log.debug("create JarFileProviderLoader: " + file);
        }
        return new JarPluginProviderLoader(file,cachedir);
    }

}
