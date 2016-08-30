/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.server.plugins.services;

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.AbstractDescribableScriptPlugin;
import com.dtolabs.rundeck.core.plugins.PluginException;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;
import com.dtolabs.rundeck.plugins.rundeck.UIPlugin;
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder;

import java.util.*;

/**
 * Created by greg on 8/30/16.
 */
public class ScriptUIPlugin extends AbstractDescribableScriptPlugin implements UIPlugin {
    List<String> paths;
    Map<String, List<String>> pathResources = new HashMap<>();
    Map<String, List<String>> pathScripts = new HashMap<>();
    Map<String, List<String>> pathStyles = new HashMap<>();

    public ScriptUIPlugin(
            final ScriptPluginProvider provider,
            final Framework framework
    )
    {
        super(provider, framework);
        loadProviderDefs(provider);
    }

    private void loadResourcesFromMap(Map uimap) {
        //pages,styles,scripts, each a string or list of strings
        List<String> pages = asStringList(uimap.get("pages"));
        List<String> styles = asStringList(uimap.get("styles"));
        List<String> scripts = asStringList(uimap.get("scripts"));
        if (null == pages) {
            throw new IllegalArgumentException(
                    "in provider metadata: 'ui: pages:' not found, or not a String or String list");
        }
        for (String page : pages) {
            if (styles != null) {
                for (String resource : styles) {
                    addResource(page, resource, pathResources);
                    addResource(page, resource, pathStyles);
                }
            }
            if (scripts != null) {
                for (String resource : scripts) {
                    addResource(page, resource, pathResources);
                    addResource(page, resource, pathScripts);
                }
            }
        }
    }

    private void addResource(final String page, final String resource, final Map<String, List<String>> dataSet) {
        if (null == dataSet.get(page)) {
            dataSet.put(page, new ArrayList<String>());
        }
        dataSet.get(page).add(resource);
    }

    private static List<String> asStringList(final Object pages) {
        if (pages instanceof String) {
            return Collections.singletonList((String) pages);
        } else if (pages instanceof Collection) {
            ArrayList<String> strings = new ArrayList<>();
            for (Object o : ((Collection) pages)) {
                if (o instanceof String) {
                    strings.add((String) o);
                }
            }
            return strings;
        }
        return null;
    }

    private void loadProviderDefs(final ScriptPluginProvider provider) {
        Map<String, Object> metadata = provider.getMetadata();
        Object ui = metadata.get("ui");
        if (ui instanceof Map) {
            Map uimap = (Map) ui;
            loadResourcesFromMap(uimap);
        } else if (ui instanceof Collection) {
            for (Object o : ((Collection) ui)) {
                if (o instanceof Map) {
                    loadResourcesFromMap((Map) o);
                }
            }
        } else {
            throw new IllegalArgumentException("in provider metadata: 'ui:' was not a Map or a List");
        }

    }

    static void validateScriptPlugin(final ScriptPluginProvider plugin) throws PluginException {
        try {
            createDescription(plugin, true, DescriptionBuilder.builder());
        } catch (ConfigurationException e) {
            throw new PluginException(e);
        }
        Map<String, Object> metadata = plugin.getMetadata();
        Object ui = metadata.get("ui");
        if (ui instanceof Map) {
            Map uimap = (Map) ui;
            List<String> pages = asStringList(uimap.get("pages"));
            if (pages == null) {
                throw new IllegalArgumentException(
                        "in provider metadata: 'ui: pages:' not found, or not a String or String list");
            }
        } else if (ui instanceof Collection) {
            for (Object o : ((Collection) ui)) {
                if (o instanceof Map) {
                    Map uimap = (Map) o;
                    List<String> pages = asStringList(uimap.get("pages"));
                    if (pages == null) {
                        throw new IllegalArgumentException(
                                "in provider metadata: 'ui: - pages:' not found, or not a String or String list");
                    }
                }
            }
        } else {
            throw new IllegalArgumentException("in provider metadata: 'ui:' was not a Map or a List");
        }
    }

    private List<String> keyAppliedForPath(String path, Map<String, List<String>> map) {

        for (String s : map.keySet()) {
            if ("*".equals(s) || s.equals(path) || path.matches(s)) {
                return map.get(s);
            }
        }
        return null;
    }

    @Override
    public boolean doesApply(final String path) {
        return keyAppliedForPath(path, pathResources) != null;
    }

    @Override
    public List<String> resourcesForPath(final String path) {
        return keyAppliedForPath(path, pathResources);
    }

    @Override
    public List<String> scriptResourcesForPath(final String path) {
        return keyAppliedForPath(path, pathScripts);
    }

    @Override
    public List<String> styleResourcesForPath(final String path) {
        return keyAppliedForPath(path, pathStyles);
    }

    @Override
    public boolean isAllowCustomProperties() {
        return false;
    }
}
