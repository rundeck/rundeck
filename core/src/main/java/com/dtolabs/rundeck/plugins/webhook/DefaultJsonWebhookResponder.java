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
package com.dtolabs.rundeck.plugins.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

public class DefaultJsonWebhookResponder implements WebhookResponder {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger LOG              = LoggerFactory.getLogger(DefaultWebhookResponder.class);
    private final Map<String, Object> responsePayload;

    public DefaultJsonWebhookResponder(Map<String,Object> responsePayload) {
        this.responsePayload = responsePayload;
    }


    @Override
    public void respond(final HttpServletResponse response) {
        try {
            response.setContentType("application/json; charset=UTF-8");
            mapper.writeValue(response.getOutputStream(),responsePayload);
        } catch(Exception ex) {
            LOG.error("Unable to write webhook response",ex);
        }
    }
}
