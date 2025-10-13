package org.rundeck.util.api.storage

import okhttp3.Response
import org.rundeck.util.container.ClientProvider
import org.rundeck.util.container.RdClient

class KeyStorageApiClient {
    private final RdClient client
    private final String STORAGE_BASE_URL = '/storage/keys'
    private final Map<String, String> keyContentTypes = [ password: 'application/x-rundeck-data-password']

    KeyStorageApiClient(ClientProvider clientProvider){
        this.client = clientProvider.client
    }

    Response callUploadKey(String path, String keyType, String keyValue){
        //test key exists
        boolean exists = false
        try (def testexists = client.doGet("${STORAGE_BASE_URL}/${path}")) {
            exists = testexists.successful
        }
        Response resp = exists ? client.doPutWithRawText(
            "${STORAGE_BASE_URL}/${path}",
            keyContentTypes[keyType.toLowerCase()],
            keyValue
        ) : client.doPostWithRawText(
            "${STORAGE_BASE_URL}/${path}",
            keyContentTypes[keyType.toLowerCase()],
            keyValue
        )
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
