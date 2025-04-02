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

import com.dtolabs.rundeck.core.plugins.JarPluginProviderLoader
import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.ResponseCodes
import com.rundeck.repository.ResponseMessage
import com.rundeck.repository.validator.ArtifactBinaryValidator


class JarPluginArtifactValidator implements ArtifactBinaryValidator {

    @Override
    ResponseBatch validate(final File artifactToValidate) {
        ResponseBatch response = new ResponseBatch()
        if(JarPluginProviderLoader.isValidJarPlugin(artifactToValidate)) {
            response.addMessage(ResponseMessage.success())
        } else {
            response.addMessage(new ResponseMessage(code: ResponseCodes.INVALID_BINARY, message:"Jar is not valid. Please check the logs for specific error messages"))
        }
        return response
    }
}
