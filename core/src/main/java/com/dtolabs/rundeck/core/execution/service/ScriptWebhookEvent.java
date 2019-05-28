/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.plugins.BaseScriptPlugin;
import com.dtolabs.rundeck.core.plugins.ScriptPluginProvider;
import com.dtolabs.rundeck.core.plugins.ServiceProviderLoader;
import com.dtolabs.rundeck.plugins.webhook.WebhookData;
import com.dtolabs.rundeck.plugins.webhook.WebhookEventPlugin;
import org.apache.log4j.Logger;

public class ScriptWebhookEvent extends BaseScriptPlugin implements WebhookEventPlugin {
    private static final Logger                LOG = Logger.getLogger(ScriptWebhookEvent.class);

    private final        ServiceProviderLoader pluginManager;

    protected ScriptWebhookEvent(
            final ScriptPluginProvider provider,
            final ServiceProviderLoader pluginManager
    ) {
        super(provider);
        this.pluginManager = pluginManager;
    }

    @Override
    public boolean isAllowCustomProperties() {
        return true;
    }

    @Override
    public void onEvent(final WebhookData data) {
        //TODO: Implement this
    }
}
