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
package testhelpers

import com.fasterxml.jackson.databind.ObjectMapper

import java.util.concurrent.Executors


class MinioTestServer {
    ObjectMapper jmap = new ObjectMapper()
    def exec =  Executors.newFixedThreadPool(2)
    Process minio
    String secretKey
    String accessKey

    void start() {
        exec.execute {
            def minioServerDir = null
            String osname = System.getProperty("os.name").toLowerCase()
            if("mac os x" == osname) {
                minioServerDir = this.getClass().getClassLoader().getResource("mac/minio")
            } else if("linux" == osname) {
                minioServerDir = this.getClass().getClassLoader().getResource("linux/minio")
            } else {
                throw new Exception("Minio Test Server cannot execute on your platform")
            }
            File confDir
            ProcessBuilder b = new ProcessBuilder("./minio","-C","/tmp/test-data/.minio","server","/tmp/test-data")
            b.directory(new File(minioServerDir.toURI()).parentFile)
            minio = b.start()

            println "Object store started: " + minio.isAlive()
        }
        Thread.sleep(2000)
        def minioConfig = jmap.readValue(new File("/tmp/test-data/.minio/config.json"),Map)
        secretKey = minioConfig.credential.secretKey
        accessKey = minioConfig.credential.accessKey
    }

    void stop() {
        minio.destroy()
        exec.shutdown()
        new File("/tmp/test-data").deleteDir()
    }
}
