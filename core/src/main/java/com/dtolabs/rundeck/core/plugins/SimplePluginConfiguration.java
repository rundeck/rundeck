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

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@Data
@Builder
@RequiredArgsConstructor
public class SimplePluginConfiguration
        implements ExtPluginConfiguration
{
    final String service;
    final String provider;
    final Map<String, Object> configuration;
    Map<String, Object> extra;

    public SimplePluginConfiguration(
            final String service,
            final String provider,
            final Map<String, Object> configuration,
            final Map<String, Object> extra
    )
    {
        this.service = service;
        this.provider = provider;
        this.configuration = configuration;
        this.extra = extra;
    }

    @Override
    public String toString() {
        return String.format("Plugin: %s:%s", getService(), getProvider());
    }
}
