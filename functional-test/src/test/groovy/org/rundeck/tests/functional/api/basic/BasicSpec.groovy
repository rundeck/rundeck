package org.rundeck.tests.functional.api.basic

import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.rundeck.util.annotations.APITest
import org.rundeck.util.container.BaseContainer
import org.rundeck.util.container.SeleniumBase
import org.rundeck.util.api.common.CustomCookieJar

@APITest
class BasicSpec extends BaseContainer {

    def setupSpec() {
        setupProject()
    }

    def testInvalidToken() {
        given:
        def client = clientWithToken("invalidtoken")
        when:
        def response = client.doGet("/system/info")
        then:
        !response.successful
        response.code() == 403
        cleanup:
        response.close()
    }

    def testSystemInfo() {
        when:
        def data = get("/system/info", Map)
        then:
        !data.error
        data.system.rundeck.apiversion.toInteger() >= 14
    }

    def testSecurityHeaders() {
        when:
        def response = doGet("/system/info")
        then:
        response.headers() != null
        response.headers().get("cache-control") != null
        response.headers().get("content-security-policy") != null
        response.headers().get("x-content-type-options") != null
        response.headers().get("x-content-type-options").contains("nosniff")
        response.headers().get("x-frame-options") != null
        response.headers().get("x-frame-options").contains("deny")
        response.headers().get("x-xss-protection") != null
        response.headers().get("strict-transport-security") != null
        response.headers().get("strict-transport-security").contains("max-age=31536000; includeSubDomains")
        response.headers().get("permissions-policy") != null
        response.headers().get("permissions-policy").contains("accelerometer=(), camera=(), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), payment=(), usb=()")
        response.headers().get("referrer-policy") != null
        response.headers().get("referrer-policy").contains("strict-origin-when-cross-origin")


    }

    def invalidUrl() {
        when:
            def data = doGet("/dnexist?project=test")
        then:
            data.code() == 404
            def json = getClient().jsonValue(data.body(), Map)
            json.error
            json.message == "Invalid API Request: /api/${client.apiVersion}/dnexist"
    }

    def jSecurityCheck() {
        when:
            def customCookieJar = new CustomCookieJar()
            def httpClient = new OkHttpClient.Builder()
                    .cookieJar(customCookieJar)
                    .build()
            def request1 = new Request.Builder()
                .url(client.baseUrl)
                .get()
                .build()
        then:
            def login1 = httpClient.newCall(request1).execute()
        when:
            FormBody formBody = new FormBody.Builder()
                .add("j_username", SeleniumBase.TEST_USER)
                .add("j_password", SeleniumBase.TEST_PASS)
                .build()
            def request2 = new Request.Builder()
                .url("${client.baseUrl}/j_security_check")
                .post(formBody)
                .build()
        then:
            def login2 = httpClient.newCall(request2).execute()
            !login2.body().string().contains("j_security_check")

            login1.close()
            login2.close()
    }
}
