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
package com.rundeck.repository.client.manifest

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.rundeck.repository.ResponseMessage
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.manifest.ManifestEntry
import com.rundeck.repository.manifest.ManifestService
import com.rundeck.repository.manifest.RundeckManifestEntry
import com.rundeck.repository.manifest.search.ManifestSearch
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit


class RundeckOfficialManifestService implements ManifestService {
    private static final String ARTIFACT_LIST = "artifact-list"
    private static final Logger LOG = LoggerFactory.getLogger(RundeckOfficialManifestService)
    private static final ObjectMapper mapper = new ObjectMapper()
    private OkHttpClient client = new OkHttpClient();
    private final String serviceEndpoint
    private Cache<String,Collection<ManifestEntry>> cache

    RundeckOfficialManifestService(String serviceEndpoint, long cacheListTime, TimeUnit cacheUnit) {
        this.serviceEndpoint = serviceEndpoint
        if(LOG.traceEnabled) LOG.trace("service endpoint: ${serviceEndpoint}")
        cache =CacheBuilder.newBuilder()
                .maximumSize(1)
                .expireAfterWrite(cacheListTime, cacheUnit)
                .build()
    }

    @Override
    ResponseMessage syncManifest() {
        //no-op
        return ResponseMessage.success()
    }

    @Override
    ManifestEntry getEntry(final String artifactId) {
        Response response
        try {
            if(LOG.traceEnabled) LOG.trace("/entry/${artifactId}")
            Request rq = new Request.Builder().method("GET", null).
                    url(serviceEndpoint + "/entry/${artifactId}".toString()).
                    build()
            response = client.newCall(rq).execute()
            if(response.isSuccessful())
                return ArtifactUtils.createManifestEntryFromInputStream(response.body().byteStream())
            else {
                LOG.error("getEntry http error: ${response.body().string()}")
                return null
            }
        } catch(Exception ex) {
            LOG.error("getEntry error",ex)
        } finally {
            if(response) response.body().close()
        }
        return null
    }

    @Override
    Collection<ManifestEntry> listArtifacts(final Integer offset, final Integer max) {
        Collection<ManifestEntry> artifactList = cache.getIfPresent(ARTIFACT_LIST)
        if(artifactList != null) return artifactList
        artifactList = []
        Response response
        try {
            Request rq = new Request.Builder()
                    .method("GET", null)
                    .url(serviceEndpoint + "/indexed-list")
                    .build()
            response = client.newCall(rq).execute()
            if(response.isSuccessful()) {
                def map = mapper.readValue(response.body().byteStream(),HashMap)

                map.hits.each { hit ->
                    RundeckManifestEntry entry = new RundeckManifestEntry()
                    entry.record = hit
                    artifactList.add(entry)
                }
                cache.put(ARTIFACT_LIST,artifactList)
                return artifactList
            } else {
                LOG.error("listArtifacts http error: ${response.body().string()}")
                return []
            }
        } catch(Exception ex) {
            LOG.error("listArtifacts error",ex)
        } finally {
            if(response) response.body().close()
        }
        return []
    }

    @Override
    Collection<ManifestEntry> searchArtifacts(final ManifestSearch search) {
        return []
    }
}
