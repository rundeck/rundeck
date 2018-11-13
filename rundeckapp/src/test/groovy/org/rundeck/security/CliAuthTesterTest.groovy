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
package org.rundeck.security

import rundeckapp.init.RundeckInitConfig
import spock.lang.Shared
import spock.lang.Specification

import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.callback.NameCallback
import javax.security.auth.callback.PasswordCallback
import javax.security.auth.callback.UnsupportedCallbackException


class CliAuthTesterTest extends Specification {
    private static final String LOGIN_MODULE_NAME = "TestModule"

    @Shared
    File testDir
    @Shared
    File testLoginModule
    @Shared
    File testRealmProps

    def setupSpec() {
        testDir = File.createTempDir()
        testLoginModule = new File(testDir, "jaas-testmodule.conf")
        testRealmProps = new File(testDir, "realm.properties")

        testRealmProps << "test:test,user"
        testLoginModule << """${LOGIN_MODULE_NAME} {
                                org.eclipse.jetty.jaas.spi.PropertyFileLoginModule required
                                file="${testRealmProps.absolutePath}";
                              };
                           """
    }

    def "TestAuth success"() {
        setup:
        System.setProperty("java.security.auth.login.config",testLoginModule.absolutePath)
        CliAuthTester authTester = new CliAuthTester()
        authTester.metaClass.getCallbackHandler = { -> new TestCallbackHandler("test","test") }
        RundeckInitConfig config = new RundeckInitConfig()
        config.loginModuleName = LOGIN_MODULE_NAME
        config.useJaas = true
        config.runtimeConfiguration = new Properties()
        config.runtimeConfiguration.setProperty("loginmodule.name",LOGIN_MODULE_NAME)

        expect:
        authTester.testAuth(config)
    }

    def "TestAuth failure"() {
        setup:
        System.setProperty("java.security.auth.login.config",testLoginModule.absolutePath)
        CliAuthTester authTester = new CliAuthTester()
        authTester.metaClass.getCallbackHandler = { -> new TestCallbackHandler("test","badpwd") }
        RundeckInitConfig config = new RundeckInitConfig()
        config.loginModuleName = LOGIN_MODULE_NAME
        config.useJaas = true
        config.runtimeConfiguration = new Properties()
        config.runtimeConfiguration.setProperty("loginmodule.name",LOGIN_MODULE_NAME)

        expect:
        !authTester.testAuth(config)
    }

    def "TestAuth fails when jaas is not enabled"() {
        setup:
        System.setProperty("java.security.auth.login.config",testLoginModule.absolutePath)
        CliAuthTester authTester = new CliAuthTester()
        authTester.metaClass.getCallbackHandler = { -> new TestCallbackHandler("test","badpwd") }
        RundeckInitConfig config = new RundeckInitConfig()
        config.loginModuleName = LOGIN_MODULE_NAME
        config.useJaas = false
        config.runtimeConfiguration = new Properties()
        config.runtimeConfiguration.setProperty("loginmodule.name",LOGIN_MODULE_NAME)

        expect:
        !authTester.testAuth(config)
    }

    class TestCallbackHandler implements CallbackHandler {

        private final String un
        private final String pwd

        TestCallbackHandler(String un, String pwd) {
            this.un = un
            this.pwd = pwd
        }

        @Override
        void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            callbacks.each {
                if(it instanceof NameCallback) {
                    NameCallback callback = (NameCallback)it
                    callback.name = un
                } else if(it instanceof PasswordCallback) {
                    PasswordCallback callback = (PasswordCallback)it
                    callback.password = pwd.toCharArray()
                }
            }
        }
    }
}
