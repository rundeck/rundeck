/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dtolabs.rundeck.core.execution.service;

import com.dtolabs.rundeck.core.plugins.BaseScriptPlugin;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.plugins.tours.TourLoaderPlugin;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

public class ScriptTourLoader extends BaseScriptPlugin implements TourLoaderPlugin {
    private static final Logger       LOG    = Logger.getLogger(ScriptTourLoader.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private final ServiceProviderLoader pluginManager;

    public ScriptTourLoader(final ScriptPluginProvider provider, final ServiceProviderLoader pluginManager) {
        super(provider);
        this.pluginManager = pluginManager;
    }

    @Override
    public boolean isAllowCustomProperties() {
        return true;
    }

    @Override
    public Map getTourManifest() {
        final ScriptPluginProvider plugin = getProvider();
        try {
            return mapper.readValue(new File(plugin.getContentsBasedir(), "resources/tour-manifest.json"),
                                    TreeMap.class);
        } catch(Exception ex) {
            LOG.error("Unable to serve tour manifest.",ex);
        }

        return new TreeMap();
    }

    @Override
    public Map getTour(final String tourId) {
        String tourKey = tourId.endsWith(".json") ? tourId : tourId +".json";
        final ScriptPluginProvider plugin = getProvider();
        try {
            return mapper.readValue(new File(plugin.getContentsBasedir(), "resources/tours/"+tourKey),
                                    TreeMap.class);
        } catch(Exception ex) {
            LOG.error("Unable to serve tour.",ex);
        }
        return new TreeMap();
    }
}
