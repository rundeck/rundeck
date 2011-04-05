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
public class PluginDef {
    Map<String, String> data;

    public PluginDef(final Map<String, String> data) {
        this.data = data;
    }

    public String getName() {
        return data.get("name");
    }


    public String getService() {
        return data.get("service");
    }

    public String getScriptFile() {
        return data.get("script-file");
    }

    public String getScriptArgs() {
        return data.get("script-args");
    }

    public String getPluginType() {
        return data.get("plugin-type");
    }

    @Override
    public String toString() {
        return "PluginDef{" +
               "data=" + data +
               '}';
    }
}
