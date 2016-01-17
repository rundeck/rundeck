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
* ScriptPluginDirScanner.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/13/11 9:59 AM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta;
import com.dtolabs.rundeck.core.utils.cache.FileCache;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.zip.ZipInputStream;

/**
 * ScriptPluginDirScanner is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ScriptPluginScanner extends DirPluginScanner {
    private static final Logger log = Logger.getLogger(ScriptPluginScanner.class.getName());
    public static final FileFilter FILENAME_FILTER = new FileFilter() {
        public boolean accept(final File file) {
            return file.isFile() && file.getName().endsWith(".zip");
        }
    };

    final File cachedir;

    public ScriptPluginScanner(final File extdir, final File cachedir,
                               final FileCache<ProviderLoader> filecache, final int rescanInterval) {
        super(extdir, filecache, rescanInterval);
        this.cachedir = cachedir;
    }

    public ProviderLoader createLoader(final File file) {
        if (log.isDebugEnabled()) {
            log.debug("create ScriptFileProviderLoader: " + file);
        }
        return new ScriptPluginProviderLoader(file, cachedir);
    }

    public ProviderLoader createCacheItemForFile(final File file) {
        return createLoader(file);
    }

    public FileFilter getFileFilter() {
        return FILENAME_FILTER;
    }

    public boolean isValidPluginFile(final File file) {
        try {
            final ZipInputStream zipinput = new ZipInputStream(new FileInputStream(file));
            final PluginMeta metadata = ScriptPluginProviderLoader.loadMeta(file, zipinput);
            zipinput.close();
            boolean valid = false;
            if(null!=metadata) {
                valid = ScriptPluginProviderLoader.validatePluginMeta(metadata, file);
            }
            if (!valid) {
                log.error("Skipping plugin file: metadata was invalid: " + file.getAbsolutePath());
            }
            return valid;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    protected String getVersionForFile(final File file) {
        return ScriptPluginProviderLoader.getVersionForFile(file);
    }
}
