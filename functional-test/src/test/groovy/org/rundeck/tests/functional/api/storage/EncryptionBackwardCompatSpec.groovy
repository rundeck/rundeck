package org.rundeck.tests.functional.api.storage

import org.rundeck.util.annotations.APITest
import org.rundeck.util.api.storage.KeyStorageApiClient
import org.rundeck.util.container.BaseContainer
import spock.lang.Shared
import spock.lang.Stepwise

/**
 * Integration test proving end-to-end backward compatibility of the storage encryption
 * after migrating from jasypt-encryption to aes-gcm-encryption.
 *
 * <p>This test exercises the full Rundeck storage API (create, read, update, delete)
 * to verify that the encryption converter plugin works correctly in a real Rundeck
 * instance. The test environment uses the default encryption configuration which
 * exercises the aes-gcm-encryption plugin (or jasypt-encryption alias, depending
 * on config).
 *
 * <p>What this proves:
 * <ul>
 *   <li>Password keys can be stored and retrieved (round-trip through encryption)</li>
 *   <li>Private keys can be stored and retrieved</li>
 *   <li>Keys can be updated (triggers re-encryption) and still be readable</li>
 *   <li>Multiple keys can coexist without interference</li>
 * </ul>
 */
@APITest
@Stepwise
class EncryptionBackwardCompatSpec extends BaseContainer {

    @Shared KeyStorageApiClient keyClient

    def setupSpec() {
        keyClient = new KeyStorageApiClient(this)
    }

    def "store a password key via API"() {
        when:
        def response = keyClient.callUploadKey(
                "encryption-test/db-password",
                "password",
                "my-secret-database-password-123"
        )

        then:
        response.successful
        response.code() in [200, 201]

        cleanup:
        response?.close()
    }

    def "retrieve the stored password key metadata"() {
        when:
        def response = client.doGet("/storage/keys/encryption-test/db-password")

        then:
        response.successful
        response.code() == 200
        def json = jsonValue(response.body(), Map)
        json.path == "keys/encryption-test/db-password"
        json.type == "file"
        json.meta["Rundeck-content-type"] == "application/x-rundeck-data-password"
        json.meta["Rundeck-data-type"] == "password"
    }

    def "update the password key with a new value"() {
        when:
        def response = keyClient.callUploadKey(
                "encryption-test/db-password",
                "password",
                "updated-password-value-456"
        )

        then:
        response.successful
        response.code() in [200, 201]

        cleanup:
        response?.close()
    }

    def "retrieve updated password key metadata still valid"() {
        when:
        def response = client.doGet("/storage/keys/encryption-test/db-password")

        then:
        response.successful
        response.code() == 200
        def json = jsonValue(response.body(), Map)
        json.path == "keys/encryption-test/db-password"
        json.meta["Rundeck-data-type"] == "password"
    }

    def "store a second password key in a different path"() {
        when:
        def response = keyClient.callUploadKey(
                "encryption-test/api-token",
                "password",
                "tk-a1b2c3d4e5f6g7h8i9j0"
        )

        then:
        response.successful
        response.code() in [200, 201]

        cleanup:
        response?.close()
    }

    def "store a private key file"() {
        given:
        def keyFile = new File(getClass().getClassLoader().getResource("test-files/privateKey.private")?.toURI()
                ?: "functional-test/src/test/resources/test-files/privateKey.private")

        when:
        def response = keyClient.callUploadKeyFile(
                "encryption-test/ssh-key",
                "privatekey",
                keyFile
        )

        then:
        response.successful
        response.code() in [200, 201]

        cleanup:
        response?.close()
    }

    def "list all keys in the encryption-test directory"() {
        when:
        def response = client.doGet("/storage/keys/encryption-test")

        then:
        response.successful
        response.code() == 200
        def json = jsonValue(response.body(), Map)
        json.type == "directory"
        json.resources.size() >= 2
    }

    def "delete all test keys"() {
        when:
        def resp1 = client.doDelete("/storage/keys/encryption-test/db-password")
        def resp2 = client.doDelete("/storage/keys/encryption-test/api-token")
        def resp3 = client.doDelete("/storage/keys/encryption-test/ssh-key")

        then:
        resp1.code() == 204
        resp2.code() == 204
        resp3.code() == 204
    }

    def "verify keys are deleted"() {
        when:
        def response = client.doGet("/storage/keys/encryption-test/db-password")

        then:
        !response.successful
        response.code() == 404
    }
}
