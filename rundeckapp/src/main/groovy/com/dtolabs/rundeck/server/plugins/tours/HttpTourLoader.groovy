/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.server.plugins.tours

import com.dtolabs.rundeck.plugins.tours.Tour
import com.dtolabs.rundeck.plugins.tours.TourLoader
import com.dtolabs.rundeck.plugins.tours.TourManifest
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response


class HttpTourLoader implements TourLoader {

    private ObjectMapper mapper = new ObjectMapper()
    private OkHttpClient client = new OkHttpClient();
    String loaderName
    String tourManifestEndpoint


    @Override
    String getLoaderName() {
        return loaderName
    }

    @Override
    TourManifest getTourManifest() {
        Response response
        try {
            Request rq = new Request.Builder().method("GET", null).
                    url(tourManifestEndpoint + "/tour-manifest.json").
                    build()
            response = client.newCall(rq).execute()
            if(response.isSuccessful()) {
                return mapper.readValue(response.body().byteStream(), TourManifestImpl)
            } else {
                println response.body().string()
            }

        } catch(Exception ex) {
            ex.printStackTrace()
        } finally {
            if(response) response.body().close()
        }
        return null
    }

    @Override
    Tour getTour(final String tourId) {
        Response response
        try {
            Request rq = new Request.Builder().method("GET", null).
                    url(tourManifestEndpoint + "/tours/"+tourId).
                    build()
            response = client.newCall(rq).execute()
            if(response.isSuccessful()) {
                return mapper.readValue(response.body().byteStream(),TourImpl)
            } else {
                println response.body().string()
            }

        } catch(Exception ex) {
            ex.printStackTrace()
        } finally {
            if(response) response.body().close()
        }
        return null
    }
}
