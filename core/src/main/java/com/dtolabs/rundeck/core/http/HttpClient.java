/*
 * Copyright 2021 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.core.http;

import java.net.URI;

public interface HttpClient<R> {

    HttpClient<R> setUri(URI uri);
    HttpClient<R> setFollowRedirects(boolean redirects);
    HttpClient<R> setBasicAuthCredentials(String username, String password);
    HttpClient<R> setTimeout(int timeoutMs);
    HttpClient<R> setMethod(Method method);
    HttpClient<R> setRetryCount(int count);
    HttpClient<R> addHeader(String name, String value);
    HttpClient<R> addPayload(String contentType, String payload);
    void execute(RequestProcessor<R> processor) throws Exception;

    enum Method {
        GET,POST,PUT,DELETE,HEAD,OPTIONS
    }
}
