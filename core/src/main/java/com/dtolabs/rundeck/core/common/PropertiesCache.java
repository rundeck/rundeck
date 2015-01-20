/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
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
* CmdPropertiesCache.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Apr 8, 2010 9:51:58 AM
* $Id$
*/
package com.dtolabs.rundeck.core.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;

/**
 * PropertiesCache caches properties file contents and reloads them if they have been modified.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class PropertiesCache {

    private HashMap<File, Long> mtimes = new HashMap<File, Long>();
    private HashMap<File, Properties> props = new HashMap<File, Properties>();

    /**
     * Returns true if the file does not exist, or has been modified since the last time it was loaded.
     * @param file File to check
     * @return true if the file needs to be reloaded.
     * @throws IOException on io error
     */
    public synchronized boolean needsReload(final File file) throws IOException {
        final long lastMod = file.lastModified();
        if (!file.exists()) {
            mtimes.remove(file);
        }
        final Long aLong = mtimes.get(file);
        return null == aLong || lastMod > aLong;
    }

    /**
     * Get the java Properties stored in the file, loading from disk only if the file has been modified
     * since the last read, or the cached data has been invalidated.
     * @param file java properties file
     * @return java Properties
     * @throws IOException due to file read or find error
     */
    public synchronized Properties getProperties(final File file) throws IOException {

        final Properties fileprops;
        if (needsReload(file)) {
            fileprops = new Properties();
            final InputStream is = new FileInputStream(file);
            try {
                fileprops.load(is);
            } finally {
                if (null != is) {
                    is.close();
                }
            }
            mtimes.put(file, file.lastModified());
            props.put(file, fileprops);
            return fileprops;
        }
        return props.get(file);
    }

    /**
     * Clear cached data for the file, causing a reload for the next call of {@link #getProperties(java.io.File)}
     *
     * @param file file
     */
    public synchronized void touch(final File file) {
        mtimes.remove(file);
        props.remove(file);
    }
}
