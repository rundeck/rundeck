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
class ScriptPluginDirScanner implements PluginDirScanner {
    private static final Logger log = Logger.getLogger(ScriptPluginDirScanner.class.getName());

    final File extdir;
    final File cachedir;
    final FileCache<FileProviderLoader> filecache;

    public ScriptPluginDirScanner(final File extdir, final File cachedir,
                                  final FileCache<FileProviderLoader> filecache) {
        this.extdir = extdir;
        this.cachedir = cachedir;
        this.filecache = filecache;
    }

    public FileProviderLoader createLoader(final File file) {
        log.debug("create ScriptFileProviderLoader: " + file);
        return new ScriptFileProviderLoader(file, cachedir);
    }

    public File scanForFile(final ProviderIdent ident) {
        debug("scanAndLoadZipScriptPlugins dir: " + extdir.getAbsolutePath());
        if (!extdir.exists() || !extdir.isDirectory()) {
            return null;
        }
        final File[] list = extdir.listFiles(getFilenameFilter());
        debug("found zip plugins: " + list.length);
        for (final File zip : list) {
            //try reading jar
            debug("test zip: " + zip.getAbsolutePath());
            if(isValid(zip)){
                final FileProviderLoader fileProviderLoader = filecache.get(zip, this);
                final boolean loaderFor = fileProviderLoader.isLoaderFor(ident);
                debug("filecache result: " + fileProviderLoader + ", loaderForIdent: " + loaderFor);
                if (null != fileProviderLoader && loaderFor) {
                    return zip;
                }
            }
        }
        return null;
    }

    public FileProviderLoader createCacheItemForFile(File file) {
        return createLoader(file);
    }

    private FilenameFilter getFilenameFilter() {
        return new FilenameFilter() {
            public boolean accept(final File file, final String s) {
                return s.endsWith("-plugin.zip");
            }
        };
    }

    private boolean isValid(final File jar) {
        try {
            final ZipInputStream zipinput = new ZipInputStream(new FileInputStream(jar));
            PluginMeta metadata = ScriptFileProviderLoader.loadMeta(jar, zipinput);
            zipinput.close();
            return ScriptFileProviderLoader.validatePluginMeta(metadata, jar);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }


    private void debug(final String msg) {
        if (log.isDebugEnabled()) {
            log.debug(msg);
        }
    }

    private void warn(final String msg) {
        log.warn(msg);
    }

    private void error(final String msg) {
        log.error(msg);
    }

}
