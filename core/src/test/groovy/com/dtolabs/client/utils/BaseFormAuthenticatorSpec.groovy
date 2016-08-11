/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.client.utils

import org.apache.commons.httpclient.HttpMethod
import spock.lang.Specification

/**
 * Created by greg on 4/20/16.
 */
class BaseFormAuthenticatorSpec extends Specification {
    def "absolute url"() {
        given:
        URL url = new URL("http", host, 8080, "/some/path")
        when:
        def result = BaseFormAuthenticator.absoluteUrl(url, location)
        then:
        result == expected

        where:
        host    | location                 | expected
        "ahost" | '/monkey'                | 'http://ahost:8080/monkey'
        "bhost" | '/monkey'                | 'http://bhost:8080/monkey'
        "ahost" | 'http://ahost2:8080/abc' | 'http://ahost2:8080/abc'

    }
}
