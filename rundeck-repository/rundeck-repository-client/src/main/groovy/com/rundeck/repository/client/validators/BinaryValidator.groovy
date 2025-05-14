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
package com.rundeck.repository.client.validators

import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.artifact.ArtifactType


class BinaryValidator {
    static JarPluginArtifactValidator jarValidator = new JarPluginArtifactValidator()
    static ScriptPluginArtifactValidator scriptValidator = new ScriptPluginArtifactValidator()
    static ResponseBatch validate(ArtifactType type, File fileToValidate) {
        if(type == ArtifactType.JAVA_PLUGIN) return jarValidator.validate(fileToValidate)
        if(type == ArtifactType.SCRIPT_PLUGIN) return scriptValidator.validate(fileToValidate)
        throw new Exception("No validator for type: ${type.name()}")
    }
}
