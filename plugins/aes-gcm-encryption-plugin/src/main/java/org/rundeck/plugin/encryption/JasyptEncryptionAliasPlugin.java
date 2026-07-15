/*
 * Copyright 2026 PagerDuty, Inc.
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

package org.rundeck.plugin.encryption;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Backward-compatible alias that registers the {@link ModernEncryptionConverterPlugin}
 * under the legacy {@code jasypt-encryption} provider name.
 *
 * <p>This allows existing installations whose {@code rundeck-config.properties} still
 * references {@code rundeck.storage.converter.*.type=jasypt-encryption} to upgrade
 * without any manual configuration changes. All behavior is inherited from
 * {@link ModernEncryptionConverterPlugin} (AES-256-GCM writes, dual-read for legacy data).
 */
@Plugin(name = ModernEncryptionConverterPlugin.JASYPT_PROVIDER_NAME, service = ServiceNameConstants.StorageConverter)
@PluginDescription(
        title = "Jasypt Storage Encryption (Legacy Compatibility)",
        description = "Backward-compatible alias for aes-gcm-encryption. " +
                "Allows existing configurations using 'jasypt-encryption' to work " +
                "without modification after upgrade. Consider migrating your config " +
                "to use 'aes-gcm-encryption' for clarity."
)
public class JasyptEncryptionAliasPlugin extends ModernEncryptionConverterPlugin {

    private static final Logger logger = LoggerFactory.getLogger(JasyptEncryptionAliasPlugin.class);
    private volatile boolean logged = false;

    private void logLegacyUsageOnce() {
        if (!logged) {
            logged = true;
            logger.warn("Using 'jasypt-encryption' provider name (legacy alias). " +
                    "Consider updating rundeck-config.properties to use 'aes-gcm-encryption'.");
        }
    }

    @Override
    public HasInputStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
                                       HasInputStream hasInputStream) {
        logLegacyUsageOnce();
        return super.readResource(path, resourceMetaBuilder, hasInputStream);
    }

    @Override
    public HasInputStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
                                         HasInputStream hasInputStream) {
        logLegacyUsageOnce();
        return super.createResource(path, resourceMetaBuilder, hasInputStream);
    }

    @Override
    public HasInputStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
                                         HasInputStream hasInputStream) {
        logLegacyUsageOnce();
        return super.updateResource(path, resourceMetaBuilder, hasInputStream);
    }
}
