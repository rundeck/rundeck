/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.app.data.providers.v1.pluginmeta;

import org.rundeck.app.data.model.v1.pluginMeta.RdPluginMeta;
import org.rundeck.app.data.providers.v1.DataProvider;

import java.util.List;
import java.util.Map;

public interface PluginMetaDataProvider extends DataProvider {
    RdPluginMeta findByProjectAndKey(String project, String key);
    List<RdPluginMeta> findAllByProjectAndKeyLike(String project, String key);
    void deleteByProjectAndKey(String project, String key);
    void deleteAllByProjectAndKeyLike(String project, String keyLike);
    Integer deleteAllByProject(String project);
    void setJobPluginMeta(String project, String key, Map metadata);
}
