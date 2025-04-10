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
package com.rundeck.repository.client.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.rundeck.repository.ResponseBatch
import com.rundeck.repository.ResponseCodes
import com.rundeck.repository.ResponseMessage
import com.rundeck.repository.artifact.RepositoryArtifact
import com.rundeck.repository.client.exceptions.ArtifactNotFoundException
import com.rundeck.repository.client.manifest.HttpManifestService
import com.rundeck.repository.client.manifest.RundeckOfficialManifestService
import com.rundeck.repository.client.signing.GpgTools
import com.rundeck.repository.client.util.ArtifactUtils
import com.rundeck.repository.definition.RepositoryDefinition
import com.rundeck.repository.api.ArtifactRepository
import com.rundeck.repository.manifest.ManifestEntry
import com.rundeck.repository.manifest.ManifestService
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit


class RundeckHttpRepository implements ArtifactRepository {
    private static final String REPO_ENDPOINT = "https://api.rundeck.com/repo/v1/oss"
    private static final String REPO_STAGING_ENDPOINT = "https://api-stage.rundeck.com/repo/v1/oss"
    private static Logger LOG = LoggerFactory.getLogger(RundeckHttpRepository)
    private OkHttpClient client = new OkHttpClient();
    private static ObjectMapper mapper = new ObjectMapper()

    private byte[] cachedPubKeyStream = null

    RepositoryDefinition repositoryDefinition
    ManifestService manifestService
    String rundeckRepositoryEndpoint

    RundeckHttpRepository(RepositoryDefinition repoDef) {
        this.rundeckRepositoryEndpoint = repoDef.configProperties.staging == true ? REPO_STAGING_ENDPOINT : REPO_ENDPOINT
        this.repositoryDefinition = repoDef
        this.manifestService = new RundeckOfficialManifestService(this.rundeckRepositoryEndpoint, 24, TimeUnit.HOURS)
    }

    @Override
    RepositoryDefinition getRepositoryDefinition() {
        return repositoryDefinition
    }

    @Override
    RepositoryArtifact getArtifact(final String artifactId, final String version = null) {
        Response response
        try {
            def manifestEntry = manifestService.getEntry(artifactId)
            if(!manifestEntry) throw new ArtifactNotFoundException("Artifact with id: ${artifactId} could not be found")
            String artifactVer = version ?: manifestEntry.currentVersion
            String artifactUrl = rundeckRepositoryEndpoint+ "/artifact/${artifactId}/${artifactVer}"
            if (LOG.traceEnabled) {
                LOG.trace("getArtifact from: " + artifactUrl)
            }
            Request rq = new Request.Builder().get().url(artifactUrl).build()
            response = client.newCall(rq).execute()
            if(response.successful) {
                return ArtifactUtils.createArtifactFromJsonStream(response.body().byteStream())
            } else {
                LOG.error("getArtifact http error: " + response.body().string())
            }
        } finally {
            if (response) {
                response.body().close()
            }
        }
    }

    @Override
    InputStream getArtifactBinary(final String artifactId, final String version = null) {
        ManifestEntry entry = manifestService.getEntry(artifactId)
        if(!entry) throw new ArtifactNotFoundException("Artifact with id: ${artifactId} could not be found")
        String artifactVer = version ?: entry.currentVersion
        String artifactUrl = rundeckRepositoryEndpoint+ "/binary/${artifactId}/${artifactVer}"
        if (LOG.traceEnabled) {
            LOG.trace("getBinary from: " + artifactUrl)
        }
        File tmp = File.createTempFile("sigchk", "bin")
        Request rqUrls = new Request.Builder().get().url(artifactUrl).build()
        Response rspUrls = null
        Response rspSig = null
        Response rspBin = null
        try {
            rspUrls = client.newCall(rqUrls).execute()
            def urls = mapper.readValue(rspUrls.body().string(), Map)
            if (LOG.traceEnabled) {
                LOG.trace("binary urls: " + urls.toString())
            }
            if(!urls.binarySigUrl || !urls.binaryUrl) throw new Exception("Binary not found")
            Request rqSig = new Request.Builder().get().url(urls.binarySigUrl).build()
            rspSig = client.newCall(rqSig).execute()
            ByteArrayInputStream sigStream = new ByteArrayInputStream(rspSig.body().bytes())
            Request rqBin = new Request.Builder().get().url(urls.binaryUrl).build()
            rspBin = client.newCall(rqBin).execute()
            tmp << rspBin.body().byteStream()
            ByteArrayInputStream pubKeyStream = getRundeckPublicKey()
            if(!GpgTools.validateSignature(tmp.newInputStream(),pubKeyStream,sigStream)) {
                throw new Exception("Cannot verify downloaded file.")
            }
        } finally {
            if (rspUrls) {
                rspUrls.close()
            }
            if (rspSig) {
                rspSig.close()
            }
            if (rspBin) {
                rspBin.close()
            }
        }

        return tmp.newInputStream()
    }

    ByteArrayInputStream getRundeckPublicKey() {
        if (cachedPubKeyStream) {
            return new ByteArrayInputStream(cachedPubKeyStream)
        }
        Request rqKey = new Request.Builder().method("GET",null).url(rundeckRepositoryEndpoint+"/verification-key").build()
        Response rspKey = null
        try {
            rspKey = client.newCall(rqKey).execute()
            if (rspKey.successful) {
                cachedPubKeyStream = rspKey.body().bytes()
            } else {
                LOG.error("getRundeckPublicKey http failure: ${rspKey.body().string()}")
                throw new Exception(
                        "Unable to verify artifact. Public Key could not be loaded." +
                        "Please ensure data from: ${rundeckRepositoryEndpoint}/verification-key can be loaded"
                )
            }
        } finally {
            if (rspKey) {
                rspKey.close()
            }
        }
        return new ByteArrayInputStream(cachedPubKeyStream)
    }

    @Override
    ResponseBatch saveNewArtifact(final RepositoryArtifact artifact) {
        return new ResponseBatch().withMessage(new ResponseMessage(code: ResponseCodes.NOT_SUPPORTED,message:"Operation Not Supported"))
    }

    @Override
    ResponseBatch uploadArtifact(final InputStream artifactInputStream) {
        return new ResponseBatch().withMessage(new ResponseMessage(code: ResponseCodes.NOT_SUPPORTED,message:"Operation Not Supported"))
    }

    @Override
    ManifestService getManifestService() {
        return manifestService
    }

    @Override
    void recreateAndSaveManifest() {
        //no-op
    }

    @Override
    boolean isEnabled() {
        return repositoryDefinition.enabled
    }
}
