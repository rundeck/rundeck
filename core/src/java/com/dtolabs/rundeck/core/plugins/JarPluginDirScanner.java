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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * JarPluginDirScanner is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class JarPluginDirScanner implements PluginDirScanner {
    static Logger log = Logger.getLogger(JarPluginDirScanner.class.getName());
    public static final String RUNDECK_PLUGIN_ARCHIVE = "Rundeck-Plugin-Archive";
    public static final String RUNDECK_PLUGIN_CLASSNAMES = "Rundeck-Plugin-Classnames";
    public static final String JAR_PLUGIN_VERSION = "1.0";
    public static final String RUNDECK_PLUGIN_VERSION = "Rundeck-Plugin-Version";

    final File extdir;
    final File cachedir;
    final FileCache<FileProviderLoader> filecache;

    JarPluginDirScanner(final File extdir, final File cachedir, final FileCache<FileProviderLoader> filecache) {
        this.extdir = extdir;
        this.cachedir = cachedir;
        this.filecache = filecache;
    }

    public boolean isValidPluginFile(final File file) {
        final JarInputStream jarInputStream;
        boolean valid = false;
        try {
            jarInputStream = new JarInputStream(new FileInputStream(file));
            final Manifest manifest = jarInputStream.getManifest();
            final Attributes mainAttributes = manifest.getMainAttributes();
            valid = validateJarManifest(file, mainAttributes);
            if (!valid) {
                debug("Skipping plugin file: metadata was invalid: " + file.getAbsolutePath());
            }
            jarInputStream.close();
        } catch (IOException e) {
            return false;
        }
        return valid;
    }

    private static boolean validateJarManifest(final File jar, final Attributes mainAttributes) {
        boolean valid = true;
        final String value1 = mainAttributes.getValue(RUNDECK_PLUGIN_ARCHIVE);
        final String plugvers = mainAttributes.getValue(RUNDECK_PLUGIN_VERSION);
        final String plugclassnames = mainAttributes.getValue(
            RUNDECK_PLUGIN_CLASSNAMES);
        if (null == value1) {
            log.warn("Jar plugin manifest attribute missing: " + RUNDECK_PLUGIN_ARCHIVE + ": " + jar.getAbsolutePath());
            valid = false;
        }
        if (null == plugvers) {
            log.warn("Jar plugin manifest attribute missing: " + RUNDECK_PLUGIN_VERSION + ": " + jar.getAbsolutePath());
            valid = false;
        } else if (!JAR_PLUGIN_VERSION.equals(plugvers)) {
            log.warn("Unssupported plugin version: " + RUNDECK_PLUGIN_VERSION + ": " + plugvers + ": " + jar
                .getAbsolutePath());
            valid = false;
        }
        if (null == plugclassnames) {
            log.warn(
                "Jar plugin manifest attribute missing: " + RUNDECK_PLUGIN_CLASSNAMES + ": " + jar.getAbsolutePath());
            valid = false;
        }
        return valid;
    }

    public FilenameFilter getFilenameFilter() {
        return new FilenameFilter() {
            public boolean accept(final File file, final String s) {
                return s.endsWith(".jar");
            }
        };
    }


    public FileProviderLoader createCacheItemForFile(final File file) {
        return createLoader(file);
    }

    public FileProviderLoader createLoader(final File file) {
        debug("create JarFileProviderLoader: " + file);
        return new JarFileProviderLoader(file);
    }

    public File scanForFile(final ProviderIdent ident) {
        debug("scanForFile: " + ident);
        final File[] files = extdir.listFiles(getFilenameFilter());
        for (final File file : files) {
            //try reading jar
            debug("test jar: " + file.getAbsolutePath());
            if (isValidPluginFile(file)) {
                final FileProviderLoader fileProviderLoader = filecache.get(file, this);

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
