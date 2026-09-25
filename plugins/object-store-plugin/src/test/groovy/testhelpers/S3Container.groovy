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

package testhelpers

import io.minio.MinioClient
import org.testcontainers.containers.GenericContainer

/**
 * Testcontainers wrapper for an S3-compatible server (RustFS) used by object-store tests.
 */
class S3Container extends GenericContainer<S3Container> {

    private static final Integer DEFAULT_PORT = 9000;
    private String accessKey
    private String secretKey

    S3Container() {
        this("rustfs/rustfs:1.0.0")
    }

    S3Container(String dockerImageName) {
        super(dockerImageName);
        withExposedPorts(DEFAULT_PORT)
        withAccess 'TEST_KEY', UUID.randomUUID().toString()
    }

    S3Container withAccess(String accessKey, String secretKey) {
        withEnv RUSTFS_ACCESS_KEY: accessKey, RUSTFS_SECRET_KEY: secretKey
        this.accessKey = accessKey
        this.secretKey = secretKey
        return self()
    }

    MinioClient client() {
        MinioClient c = MinioClient.builder()
                .endpoint("http://${containerIpAddress}:${firstMappedPort}")
                .credentials(accessKey, secretKey)
                .build()

        // Waiting a little gives time for the container to start and become ready to accept requests.
        // Invoking operations on the container that is not ready results in:
        // errors such as 503 Server not initialized.
        S3TestUtils.ensureS3ServerInitialized(c)

        return c
    }

    @Override
    void close() {
        super.close()
    }
}
