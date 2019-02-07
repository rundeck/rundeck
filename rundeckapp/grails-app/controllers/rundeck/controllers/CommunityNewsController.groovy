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
package rundeck.controllers

import grails.converters.JSON
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response

class CommunityNewsController {

    OkHttpClient client = new OkHttpClient()

    def index() { }

    def register() {
        if(!request.JSON.email) {
            response.status = 400
            render ([msg:"Email parameter blank"]) as JSON
            return
        }
        String hubspotUrl = "https://api.hsforms.com/submissions/v3/integration/submit/2768099/da27deaa-41d8-4a10-8eed-72c2e77d54e9"

        def jsonPayload = [fields:[[name:"email",value:request.JSON.email]]] as JSON

        RequestBody payload = RequestBody.create(MediaType.parse("application/json"), jsonPayload.toString())
        Request rq = new Request.Builder().url(hubspotUrl).post(payload).build()
        Response rsp = client.newCall(rq).execute()
        if(rsp.successful) {
            rsp.body().close()
            render ([inlineMessage:"Registration Successful!"]) as JSON
        } else {
            rsp.body().close()
            response.status = 400
            render ([msg:"Registration not successful"]) as JSON
        }

    }
}

