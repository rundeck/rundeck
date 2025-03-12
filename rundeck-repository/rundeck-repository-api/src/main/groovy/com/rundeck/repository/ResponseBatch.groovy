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


class ResponseBatch {
    String batchId
    private Collection<ResponseMessage> messages = []

    ResponseBatch(String batchId) {
        this.batchId = batchId
    }
    ResponseBatch() {
        this(UUID.randomUUID().toString().substring(26))
    }

    void addMessage(ResponseMessage msg) {
        msg.batchId = batchId
        messages.add(msg)
    }

    Collection<ResponseMessage> getMessages() {
        return messages
    }

    boolean batchSucceeded() {
        messages.findAll { it.code == ResponseCodes.SUCCESS }.size() == messages.size()
    }

    ResponseBatch withMessage(final ResponseMessage responseMessage) {
        addMessage(responseMessage)
        this
    }
}
