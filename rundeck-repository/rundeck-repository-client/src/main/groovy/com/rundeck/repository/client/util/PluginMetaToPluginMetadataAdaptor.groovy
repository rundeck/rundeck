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
package com.rundeck.repository.client.util

import com.dtolabs.rundeck.core.plugins.PluginMetadata
import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta
import com.dtolabs.rundeck.core.plugins.metadata.ProviderDef

import java.text.SimpleDateFormat
import java.time.Instant


class PluginMetaToPluginMetadataAdaptor implements PluginMetadata {

    PluginMeta meta

    PluginMetaToPluginMetadataAdaptor(PluginMeta meta) {
        this.meta = meta
    }

    @Override
    String getFilename() {
        return null
    }

    @Override
    File getFile() {
        return null
    }

    @Override
    String getPluginArtifactName() {
        return meta.name
    }

    @Override
    String getPluginAuthor() {
        return meta.author
    }

    @Override
    String getPluginFileVersion() {
        return meta.version
    }

    @Override
    String getPluginVersion() {
        return meta.rundeckPluginVersion
    }

    @Override
    String getPluginUrl() {
        return null
    }

    @Override
    Date getPluginDate() {
        if(!meta.date) return null
        try {
            return new Date(Instant.parse(meta.date).toEpochMilli())
        } catch(Exception ex) {}
        try {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX").parse(meta.date)
        } catch(Exception ex) {}
        return null
    }

    @Override
    Date getDateLoaded() {
        return null
    }

    @Override
    String getPluginName() {
        return meta.name
    }

    @Override
    String getPluginDescription() {
        return meta.description
    }

    @Override
    String getPluginId() {
        return PluginUtils.generateShaIdFromName(meta.name)
    }

    @Override
    String getRundeckCompatibilityVersion() {
        return meta.rundeckCompatibilityVersion
    }

    @Override
    String getTargetHostCompatibility() {
        return meta.targetHostCompatibility
    }

    @Override
    List<String> getTags() {
        return meta.tags
    }

    @Override
    String getPluginLicense() {
        return meta.license
    }

    @Override
    String getPluginThirdPartyDependencies() {
        return meta.thirdPartyDependencies
    }

    @Override
    String getPluginSourceLink() {
        return meta.sourceLink
    }

    @Override
    String getPluginDocsLink() {
        return meta.docsLink
    }

    @Override
    String getPluginType() {
        return "script"
    }

    List<ProviderDef> pluginDefs() {
        return meta.pluginDefs
    }

}
