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

class MinioContainer extends GenericContainer<MinioContainer> {

    private static final Integer DEFAULT_PORT = 9000;
    private String accessKey
    private String secretKey

    MinioContainer() {
        this("minio/minio:RELEASE.2019-09-18T21-55-05Z")
    }

    MinioContainer(String dockerImageName) {
        super(dockerImageName);
        withExposedPorts(DEFAULT_PORT)
        withCommand('server /data')
        withAccess 'TEST_KEY', UUID.randomUUID().toString()
    }

    MinioContainer withAccess(String accessKey, String secretKey) {
        withEnv MINIO_ACCESS_KEY: accessKey, MINIO_SECRET_KEY: secretKey
        this.accessKey = accessKey
        this.secretKey = secretKey
        return self()
    }

    MinioClient client() {
        new MinioClient("http://localhost:9000", "TEST_KEY", "TEST_PASSWORD")
    }

    @Override
    void close() {
        super.close()
    }
}
