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
* ScriptPluginProviderImpl.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/5/11 11:09 AM
* 
*/
package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta;
import com.dtolabs.rundeck.core.plugins.metadata.ProviderDef;

import java.io.File;
import java.util.Map;

/**
 * Contains PluginDef and info about archive file/cache dir to implement ScriptPluginProvider
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
class ScriptPluginProviderImpl implements ScriptPluginProvider {
    final private ProviderDef plugindef;
    final private File archiveFile;
    final private File scriptFile;
    final private File contentsBasedir;
    final private PluginMeta pluginMeta;

    ScriptPluginProviderImpl(PluginMeta pluginMeta,final ProviderDef plugindef, final File archiveFile, final File basedir) {
        this.pluginMeta = pluginMeta;
        this.plugindef = plugindef;
        this.archiveFile = archiveFile;
        this.contentsBasedir=basedir;
        scriptFile = new File(basedir, plugindef.getScriptFile());
    }

    public String getName() {
        return plugindef.getName();
    }

    public String getService() {
        return plugindef.getService();
    }

    public File getArchiveFile() {
        return archiveFile;
    }

    public String getScriptArgs() {
        return plugindef.getScriptArgs();
    }

    public File getScriptFile() {
        return scriptFile;
    }

    public String getScriptInterpreter() {
        return plugindef.getScriptInterpreter();
    }

    public boolean getInterpreterArgsQuoted() {
        return plugindef.getInterpreterArgsQuoted();
    }

    public Map<String, Object> getMetadata() {
        return plugindef.getPluginData();
    }

    public File getContentsBasedir() {
        return contentsBasedir;
    }

    public PluginMeta getPluginMeta() {
        return pluginMeta;
    }

    @Override
    public boolean getDefaultMergeEnvVars() {
        return ScriptPluginProviderLoader.getDefaultMergeEnvVars(pluginMeta);
    }
}
