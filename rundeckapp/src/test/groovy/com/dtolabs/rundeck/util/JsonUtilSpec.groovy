/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.util

import jakarta.servlet.http.HttpServletRequest
import spock.lang.Specification
import spock.lang.Unroll

class JsonUtilSpec extends Specification {

    HttpServletRequest mockRequest(String contentType, String body) {
        Mock(HttpServletRequest) {
            getContentType() >> contentType
            getReader() >> new BufferedReader(new StringReader(body ?: ''))
        }
    }

    def "parseRequestBody parses a JSON object body"() {
        given:
        def request = mockRequest('application/json', '{"a":"b"}')

        expect:
        JsonUtil.parseRequestBody(request) == [a: 'b']
    }

    @Unroll
    def "parseRequestBody returns null for #reason"() {
        given:
        def request = mockRequest(contentType, body)

        expect:
        JsonUtil.parseRequestBody(request) == null

        where:
        reason                  | contentType        | body
        'a JSON array body'     | 'application/json' | '["a","b"]'
        'a non-json contentType'| 'text/plain'       | '{"a":"b"}'
        'an empty body'         | 'application/json' | ''
    }

    def "parseRequestBodyAsList parses a JSON array body"() {
        // Reproduces the legacy job list page's POST /menu/listExport request body (RUN-10468),
        // a bare array of job ids rather than a JSON object.
        given:
        def request = mockRequest('application/json', '["7add463a-93c1-4936-9ac0-01a25321554f","3b66b5b7-c066-46f9-9c4d-b448cb7cb990"]')

        expect:
        JsonUtil.parseRequestBodyAsList(request) == [
            '7add463a-93c1-4936-9ac0-01a25321554f',
            '3b66b5b7-c066-46f9-9c4d-b448cb7cb990'
        ]
    }

    @Unroll
    def "parseRequestBodyAsList returns null for #reason"() {
        given:
        def request = mockRequest(contentType, body)

        expect:
        JsonUtil.parseRequestBodyAsList(request) == null

        where:
        reason                   | contentType        | body
        'a JSON object body'     | 'application/json' | '{"a":"b"}'
        'a non-json contentType' | 'text/plain'       | '["a"]'
        'an empty body'          | 'application/json' | ''
    }
}
