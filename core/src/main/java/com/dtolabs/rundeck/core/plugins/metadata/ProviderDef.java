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
* Representation.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 4/4/11 6:15 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.metadata;

import java.util.*;

/**
 * PluginDef loaded from plugin metadata
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class ProviderDef {
    public static final String META_NAME = "name";
    public static final String META_SERVICE = "service";
    public static final String META_SCRIPT_FILE = "script-file";
    public static final String META_SCRIPT_ARGS = "script-args";
    public static final String META_SCRIPT_INTERPRETER = "script-interpreter";
    public static final String META_INTERPRETER_ARGS_QUOTED = "interpreter-args-quoted";
    public static final String META_PLUGIN_TYPE = "plugin-type";
    private Map<String, Object> pluginData;

    public ProviderDef(final Map<String, Object> pluginData) {
        this.pluginData = pluginData;
    }
    private String getString(final String key){

        final Object o = pluginData.get(key);
        if(o instanceof String){
            return (String)o;
        }
        return null;
    }

    public String getName() {
        return getString(META_NAME);
    }


    public String getService() {
        return getString(META_SERVICE);
    }

    public String getScriptFile() {
        return getString(META_SCRIPT_FILE);
    }

    public String getScriptArgs() {
        return getString(META_SCRIPT_ARGS);
    }
    public String getScriptInterpreter() {
        return getString(META_SCRIPT_INTERPRETER);
    }

    public boolean getInterpreterArgsQuoted() {
        if(pluginData.get(META_INTERPRETER_ARGS_QUOTED) instanceof Boolean) {
            return (Boolean) pluginData.get(META_INTERPRETER_ARGS_QUOTED);
        }
        return Boolean.parseBoolean(getString(META_INTERPRETER_ARGS_QUOTED));
    }

    public String getPluginType() {
        return getString(META_PLUGIN_TYPE);
    }

    @Override
    public String toString() {
        return "ProviderDef{" +
               "data=" + pluginData +
               '}';
    }

    public Map<String, Object> getPluginData() {
        return pluginData;
    }
}
