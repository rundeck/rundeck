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
package testhelper

import org.springframework.boot.SpringApplication
import org.springframework.boot.SpringApplicationRunListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.EnvironmentAware
import org.springframework.core.Ordered
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.Environment
import rundeckapp.init.prebootstrap.InitializeRundeckPreboostrap

class TestAppInitializer implements SpringApplicationRunListener, Ordered {


    TestAppInitializer(SpringApplication application, String[] args) {}

    @Override
    int getOrder() {
        return 1
    }

    //@Override
    void starting() {
    }

    //@Override
    void environmentPrepared(final ConfigurableEnvironment environment) {
        new InitializeRundeckPreboostrap().run()
    }

    @Override
    void contextPrepared(final ConfigurableApplicationContext context) {

    }

    @Override
    void contextLoaded(final ConfigurableApplicationContext context) {

    }

    @Override
    void started(final ConfigurableApplicationContext context) {

    }

    @Override
    void running(final ConfigurableApplicationContext context) {

    }

    @Override
    void failed(final ConfigurableApplicationContext context, final Throwable exception) {

    }
}
