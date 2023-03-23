package org.rundeck.util

import com.dtolabs.rundeck.core.http.HttpClient
import org.apache.http.HttpResponse

interface HttpClientCreator {

    HttpClient<HttpResponse> createClient()

}