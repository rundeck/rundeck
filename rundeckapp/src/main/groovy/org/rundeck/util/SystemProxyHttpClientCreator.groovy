package org.rundeck.util

import com.dtolabs.rundeck.core.http.HttpClient
import org.apache.http.HttpResponse
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
class SystemProxyHttpClientCreator implements HttpClientCreator {
    @Override
    HttpClient<HttpResponse> createClient() {
        return new SystemProxyHttpClient()
    }
}
