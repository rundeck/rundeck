/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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
package webhooks.responder

import com.dtolabs.rundeck.plugins.webhook.WebhookResponder

import javax.servlet.http.HttpServletResponse


class DefaultJsonWebhookResponder implements WebhookResponder {

    public static final String DEFAULT_JSON_RESPONSE = '{"msg":"ok"}'

    @Override
    void respond(HttpServletResponse response) {
        response.contentType = "application/json; charset=UTF-8"
        response.contentLength = DEFAULT_JSON_RESPONSE.bytes.length
        response.outputStream << DEFAULT_JSON_RESPONSE
    }
}
