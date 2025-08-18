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
package com.rundeck.repository.client

import com.dtolabs.rundeck.core.plugins.PluginUtils
import com.rundeck.repository.artifact.ArtifactType
import com.rundeck.repository.artifact.SupportLevel
import com.rundeck.repository.manifest.ArtifactManifest
import com.rundeck.repository.manifest.ManifestEntry

class TestUtils {


    static ManifestEntry createEntry(String name, Map artifactProps = [:] ) {
        Map props = [:]
        props.id = PluginUtils.generateShaIdFromName(name)
        props.name = name
        props.description = "Rundeck plugin"
        props.artifactType = ArtifactType.JAVA_PLUGIN
        props.author = "rundeck"
        props.currentVersion = "1.0"
        props.support = SupportLevel.RUNDECK
        props.tags = ["rundeck","orignal"]
        props.putAll(artifactProps)
        return new ManifestEntry(props)
    }
    static ArtifactManifest createTestManifest () {
        ArtifactManifest manifest = new ArtifactManifest()
        manifest.entries.add(createEntry("Script Plugin",[tags:["rundeck","bash","script"]]))
        manifest.entries.add(createEntry("Copy File Plugin"))
        manifest.entries.add(createEntry("Git Plugin"))
        manifest.entries.add(createEntry("Super Notifier",["description":"10 different methods to notify job status","author":"Know Itall","support":"COMMUNITY","tags":["notification"]]))
        manifest.entries.add(createEntry("Log Enhancer",["description":"Put anything in your output logs","author":"Log Master","support":"COMMUNITY","tags":["log writer"]]))
        manifest.entries.add(createEntry("Humorous",["description":"Inject today's xkcd commic into your project page","author":"Funny Man","support":"COMMUNITY","artifactType":"SCRIPT_PLUGIN","tags":["ui"]]))
        manifest.entries.add(createEntry("Bash It",["description":"Enhance your job with bash","author":"Bash Commander","support":"COMMUNITY","artifactType":"SCRIPT_PLUGIN","tags":["bash"]]))
        manifest.entries.add(createEntry("Javascript Runner",["description":"Write javascript to do your work","author":"JS Master","support":"COMMUNITY","artifactType":"SCRIPT_PLUGIN","tags":["js","script"]]))
        manifest
    }

}
