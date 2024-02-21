package org.rundeck.util.api.storage

import okhttp3.Response
import org.rundeck.util.api.RundeckResponse
import org.rundeck.util.api.scm.httpbody.GitExportSetupRequest
import org.rundeck.util.api.scm.httpbody.SetupIntegrationResponse
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
        return client.doPostWithRawText("${STORAGE_BASE_URL}/${path}", keyContentTypes[keyType.toLowerCase()], keyValue)
    }
}
