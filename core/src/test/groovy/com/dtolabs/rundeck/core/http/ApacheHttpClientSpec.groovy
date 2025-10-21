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

    static class RecordingProxySelector extends ProxySelector {
        List<URI> seen = []
        List<Proxy> toReturn
        RecordingProxySelector(List<Proxy> proxies) { this.toReturn = proxies }

        @Override
        List<Proxy> select(URI uri) {
            seen << uri
            return toReturn
        }
        @Override
        void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            // not needed
        }
    }

    def "ApacheHttpClient consults ProxySelector for HTTPS requests"() {
        given:
        // Make ProxySelector visible to SystemDefaultRoutePlanner (added in your client)
        def old = ProxySelector.getDefault()
        def selector = new RecordingProxySelector([Proxy.NO_PROXY])
        ProxySelector.setDefault(selector)

        and: "a target URI served by MockWebServer (we won't rely on the proxy actually routing)"
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"))
        def targetUri = server.url("/proxy-check").uri()
        ApacheHttpClient client = new ApacheHttpClient()
        client.setUri(targetUri)
        client.setTimeout(200)

        when:
        String body = null
        client.execute { rsp -> body = rsp.entity.content.text }
        // let MockWebServer receive the request so the client had to plan a route
        server.takeRequest(500, TimeUnit.MILLISECONDS)

        then:
        body == "ok"
        // The key assertion: our ProxySelector was consulted for this URI
        selector.seen.any { it.host == targetUri.host && it.scheme == targetUri.scheme }

        cleanup:
        ProxySelector.setDefault(old)
    }

    def "ApacheHttpClient sees HTTP proxy from ProxySelector (even if proxy is unreachable)"() {
        given:
        def old = ProxySelector.getDefault()
        // Return a fake HTTP proxy to prove selection happens; connection may fail fast, which is fine
        def selector = new RecordingProxySelector(
                [ new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 6553)) ] // no proxy there
        )
        ProxySelector.setDefault(selector)

        and:
        def targetUri = new URI("https://example.invalid/")  // won't resolve; we only need route planning to trigger
        ApacheHttpClient client = new ApacheHttpClient()
        client.setUri(targetUri)
        client.setTimeout(100) // fail fast

        when:
        try {
            client.execute { HttpResponse r -> /* no-op */ }
            assert false: "should not succeed against an unreachable proxy/host"
        } catch (Throwable ignore) { /* expected */ }

        then:
        // Even though connect failed, the ProxySelector must have been consulted first
        selector.seen.any { it.host == targetUri.host && it.scheme == targetUri.scheme }

        cleanup:
        ProxySelector.setDefault(old)
    }

}
