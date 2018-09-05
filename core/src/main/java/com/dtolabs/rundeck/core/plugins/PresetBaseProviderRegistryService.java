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

package com.dtolabs.rundeck.core.plugins;

import com.dtolabs.rundeck.core.common.Framework;

import java.util.Map;

public class PresetBaseProviderRegistryService<T>
    extends BaseProviderRegistryService<T>
{

    private   String                              name;
    protected Map<String, Class<? extends T>> presetValues;

    public PresetBaseProviderRegistryService(
        final Map<String, Class<? extends T>> presetValues,
        final Framework framework,
        final boolean cacheInstances,
        final String name
    )
    {
        super(presetValues, framework, cacheInstances);
        this.name = name;
        this.presetValues = presetValues;
    }

    @Override
    public String getName() {
        return name;
    }
}
