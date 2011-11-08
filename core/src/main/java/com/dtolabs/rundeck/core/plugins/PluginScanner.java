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
* PluginDirScanner.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/12/11 3:52 PM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.utils.cache.FileCache;

import java.io.File;
import java.util.List;

/**
 * PluginScanner can scan some set of files for a plugin that supplies a given provider, and can create a {@link
 * ProviderLoader} as a {@link FileCache.ItemCreator}.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
interface PluginScanner extends FileCache.ItemCreator<ProviderLoader> {
    /**
     * Create a loader for a file
     */
    public ProviderLoader createLoader(File file);

    /**
     * Return a file plugin that can supply the given provider ident
     */
    public File scanForFile(ProviderIdent ident) throws PluginScannerException;
    
    /**
     * List available providers
     */
    public List<ProviderIdent> listProviders();

    /**
     * Return true if the ident and file pair is no longer valid
     */
    public boolean isExpired(final ProviderIdent ident, final File file) ;

    /**
     * Return true if the scanner determines need to rescan
     */
    public boolean shouldRescan();
}
