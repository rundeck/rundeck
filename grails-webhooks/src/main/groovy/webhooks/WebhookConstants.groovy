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
package webhooks

import com.dtolabs.rundeck.core.authorization.AuthorizationUtil


class WebhookConstants {
    public static final Map<String, String> RESOURCE_TYPE_WEBHOOK = Collections.unmodifiableMap(AuthorizationUtil.resourceType("webhook"))
    public static final String ACTION_ADMIN = "admin"
    public static final String ACTION_CREATE= "create"
    public static final String ACTION_UPDATE = "update"
    public static final String ACTION_READ = "read"
    public static final String ACTION_DELETE = "delete"
    public static final String ACTION_POST = "post"
}
