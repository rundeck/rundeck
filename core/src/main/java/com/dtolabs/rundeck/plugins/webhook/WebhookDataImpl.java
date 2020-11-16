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
package com.dtolabs.rundeck.plugins.webhook;

import lombok.Data;

import java.io.InputStream;
import java.util.HashMap;
import java.util.UUID;

/**
 * Default implementation of WebhookData
 */
@Data
public class WebhookDataImpl implements WebhookData {
    private String                 id = UUID.randomUUID().toString();
    private long                   timestamp;
    private String                 sender;
    private String                 webhook;
    private String                 webhookUUID;
    private String                 project;
    private String                 contentType;
    private InputStream            data;
    private HashMap<String,String> headers = new HashMap<>();
}
