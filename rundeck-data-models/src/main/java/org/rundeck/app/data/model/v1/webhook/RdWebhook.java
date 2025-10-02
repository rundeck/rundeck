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
package org.rundeck.app.data.model.v1.webhook;

public interface RdWebhook {
    Long getId();
    String getUuid();
    String getName();
    String getProject();
    String getAuthToken();
    String getAuthConfigJson();
    String getEventPlugin();
    String getPluginConfigurationJson();
    boolean getEnabled();
    void setUuid(String uuid);
    void setName(String name);
    void setProject(String project);
    void setAuthToken(String authTpken);
    void setAuthConfigJson(String authConfigJson);
    void setEventPlugin(String eventPlugin);
    void setPluginConfigurationJson(String plugingConfigurationJson);
    void setEnabled(boolean enabled);
}
