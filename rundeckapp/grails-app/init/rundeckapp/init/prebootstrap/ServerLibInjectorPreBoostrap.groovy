/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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
package rundeckapp.init.prebootstrap

import org.rundeck.app.bootstrap.PreBootstrap
import rundeckapp.Application


class ServerLibInjectorPreBoostrap implements PreBootstrap {
    @Override
    void run() {
        File serverLib = new File(Application.rundeckConfig.serverBaseDir, "lib")
        if (serverLib.exists()) {
            serverLib.eachFile { file ->
                if (!file.name.startsWith("rundeck-core") && file.name.endsWith(".jar")) {
                    Thread.currentThread().contextClassLoader.addURL(file.toURI().toURL())
                }
            }
        }
    }

    @Override
    float getOrder() {
        return 2
    }
}
