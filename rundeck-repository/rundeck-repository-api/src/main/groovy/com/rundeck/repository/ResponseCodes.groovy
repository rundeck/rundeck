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
package com.rundeck.repository


class ResponseCodes {
    static final String SUCCESS                                 = "operation.succeeded"
    static final String NOT_SUPPORTED                           = "operation.not.supported"
    static final String META_VALIDATION_FAILURE                 = "artifact.meta.validation.failure"
    static final String INVALID_BINARY                          = "artifact.binary.invalid"
    static final String META_UPLOAD_FAILED                      = "artifact.meta.upload.failure"
    static final String BINARY_UPLOAD_FAILED                    = "artifact.binary.upload.failure"
    static final String MANIFEST_EXISTS                         = "manifest.already.exists"
    static final String MANIFEST_SYNC_FAILURE                   = "manifest.sync.failure"
    static final String TEMPLATE_GENERATION_FAILED              = "template.generation.failed"
    static final String TEMPLATE_GENERATION_LOCATION_EXISTS     = "template.destination.exists"
    static final String REPO_DOESNT_EXIST                       = "repository.does.not.exist"
    static final String INSTALL_FAILED                          = "artifact.install.failed"

    static final String SERVER_ERROR                            = "server.error"
    static final String ARTIFACT_SIGNING_FAILED                 = "artifact.signing.failed"
}
