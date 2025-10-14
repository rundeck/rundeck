package org.rundeck.util.api.storage

import groovy.transform.CompileStatic
import okhttp3.Response
import org.rundeck.util.container.ClientProvider
import org.rundeck.util.container.RdClient

import java.util.function.Function

@CompileStatic
class KeyStorageApiClient {
    private final RdClient client
    private final String STORAGE_BASE_URL = '/storage/keys'
    private final Map<String, String> keyContentTypes = [
        password  : 'application/x-rundeck-data-password',
        privatekey: "application/octet-stream"
    ]

    KeyStorageApiClient(ClientProvider clientProvider){
        this.client = clientProvider.client
    }

    Response callUploadKeyFile(String path, String keyType, File content) {
        return callUploadKey(path) {
            it ?
            client.put("${STORAGE_BASE_URL}/${path}", content, keyContentTypes[keyType.toLowerCase()]) :
            client.post("${STORAGE_BASE_URL}/${path}", content, keyContentTypes[keyType.toLowerCase()])
        }
    }
    Response callUploadKey(String path, String keyType, String keyValue){
        return callUploadKey(path) {
            it ? client.doPutWithRawText(
                "${STORAGE_BASE_URL}/${path}",
                keyContentTypes[keyType.toLowerCase()],
                keyValue
            ) : client.doPostWithRawText(
                "${STORAGE_BASE_URL}/${path}",
                keyContentTypes[keyType.toLowerCase()],
                keyValue
            )
        }

    }

    Response callUploadKey(String path, Function<Boolean, Response> putOrPostFunction) {
        //test key exists
        boolean exists = false
        try (def testexists = client.doGet("${STORAGE_BASE_URL}/${path}")) {
            exists = testexists.successful
        }
        Response resp = putOrPostFunction(exists)
        try (def response = resp) {
            if (!response.successful) {
                throw new IOException(
                    "Failed to ${exists ? 'update' : 'create'} key to rundeck: ${resp.code()} ${resp.body().string()}"
                )
            }
            return resp
        }

    }
}
