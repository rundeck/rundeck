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
import rundeckapp.cli.CommandLineSetup
import rundeckapp.init.RundeckInitConfig
import rundeckapp.init.RundeckInitializer


class InitializeRundeckPreboostrap implements PreBootstrap {
    @Override
    void run() {
        Application.rundeckConfig = new RundeckInitConfig()
        Application.rundeckConfig.cliOptions = new CommandLineSetup().runSetup()
        new RundeckInitializer(Application.rundeckConfig).initialize()
    }

    @Override
    float getOrder() {
        return 0
    }

}
