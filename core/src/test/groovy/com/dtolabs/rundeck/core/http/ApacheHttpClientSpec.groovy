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
package com.dtolabs.rundeck.core.http

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.apache.http.HttpResponse
import spock.lang.Shared
import spock.lang.Specification

import java.util.concurrent.TimeUnit

class ApacheHttpClientSpec extends Specification {

    @Shared
    MockWebServer server

    def setupSpec() {
        server = new MockWebServer()
        server.start()
    }

    def cleanupSpec() {
        server.shutdown()
    }

    def "Execute"() {
        when:
        MockResponse mrsp = new MockResponse().setResponseCode(200).setBody("This is a test")
        server.enqueue(mrsp)
        ApacheHttpClient client = new ApacheHttpClient()
        client.setUri(server.url("test.txt").uri())
        client.addHeader("custom","my-val")
        String actual = null
        client.execute { rsp ->
            actual = rsp.getEntity().content.text
        }
        RecordedRequest request = server.takeRequest();

        then:
        actual == "This is a test"
        !request.headers.get("Authorization")
        request.headers.get("custom") == "my-val"
    }

    def "Execute With BasicAuth"() {
        when:
        MockResponse mrsp = new MockResponse().setResponseCode(200).setBody("This is a test")
        server.enqueue(mrsp)
        ApacheHttpClient client = new ApacheHttpClient();
        client.setUri(server.url("responder").uri())
        String user = "auser"
        String pwd = "apassword"
        client.setBasicAuthCredentials(user,pwd)
        String actual = null
        client.execute { rsp ->
            actual = rsp.getEntity().content.text
        }
        RecordedRequest request = server.takeRequest();
        String authHeaderVal = request.headers.get("Authorization")
        String creds = new String(Base64.decoder.decode(authHeaderVal.split(" ")[1]))

        then:
        actual == "This is a test"
        authHeaderVal == "Basic YXVzZXI6YXBhc3N3b3Jk"
        creds == "${user}:${pwd}"
    }

    def "Test Post"() {
        when:
        String responseStr = "Got the data"
        server.enqueue(new MockResponse().setResponseCode(200).setBody(responseStr))
        ApacheHttpClient client = new ApacheHttpClient()
        client.setUri(server.url("/post-target").uri())
        client.setMethod(HttpClient.Method.POST)
        String payload = '{"data":"something important"}'
        client.addPayload("application/json",payload)
        String out = null
        client.execute { rsp ->
            out = rsp.entity.content.text
        }
        RecordedRequest postRq = server.takeRequest()

        then:
        postRq.body.readUtf8() == payload
        postRq.method == "POST"
        postRq.headers.get("Content-Type") == "application/json; charset=UTF-8"

    }

    def "set retry test"() {
        when:
        HttpClient check = new RetryCheckVerifyApacheClientHttp()
        check.setRetryCount(3)

        then:
        check.retryWasAdded

    }

    class RetryCheckVerifyApacheClientHttp extends ApacheHttpClient {

        boolean retryWasAdded = false

        @Override
        HttpClient<HttpResponse> setRetryCount(final int count) {
            retryWasAdded = true
            return super.setRetryCount(count)
        }
    }

    def "timeout test"() {
        when:
        server.enqueue(new MockResponse().setBody("Will never be received").setBodyDelay(1, TimeUnit.SECONDS).setResponseCode(200))
        ApacheHttpClient client = new ApacheHttpClient()
        client.setUri(server.url("/slow-responder").uri())
        client.setTimeout(250)
        String out = null
        client.execute {rsp -> out = rsp.entity.content.text }

        then:
        thrown(SocketTimeoutException)
        !out
    }

}
