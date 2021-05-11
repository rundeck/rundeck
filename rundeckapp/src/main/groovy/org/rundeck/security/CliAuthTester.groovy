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

import org.eclipse.jetty.jaas.JAASLoginService
import rundeckapp.init.RundeckInitConfig

import javax.security.auth.callback.Callback
import javax.security.auth.callback.CallbackHandler
import javax.security.auth.callback.NameCallback
import javax.security.auth.callback.PasswordCallback
import javax.security.auth.callback.UnsupportedCallbackException
import javax.security.auth.login.LoginContext
import javax.security.auth.login.LoginException


class CliAuthTester {

    boolean testAuth(RundeckInitConfig config) {
        if(!config.useJaas) {
            println "Auth Tester can only test Jaas Authentication. Please enable Jaas auth."
            return false
        }
        String loginModule = config.loginModuleName
        if(! System.getProperty("java.security.auth.login.config"))
            System.setProperty("java.security.auth.login.config", new File(config.configDir,
                                                                           config.runtimeConfiguration.getProperty("loginmodule.conf.name")).getAbsolutePath());
        System.setProperty("loginmodule.name", config.runtimeConfiguration.getProperty("loginmodule.name"));
        println "Checking file: " + System.getProperty("java.security.auth.login.config")
        println "Checking login module: " + System.getProperty("loginmodule.name")

        LoginContext ctx = new LoginContext(loginModule,getCallbackHandler())

        try {
            def jls = new JAASLoginService()
            jls.start()
            JAASLoginService.INSTANCE.set(jls)
            ctx.login()
            println "Login Succeeded!"
            return true
        } catch(LoginException ex) {
            println "Login Failed!"
            ex.printStackTrace()
        }
        return false
    }

    CallbackHandler getCallbackHandler() {
        return new ConsoleCallbackHandler()
    }

    class ConsoleCallbackHandler implements CallbackHandler {

        @Override
        void handle(final Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            callbacks.each {
                if(it instanceof NameCallback) {
                    NameCallback callback = (NameCallback)it
                    callback.name = System.console().readLine(callback.prompt+": ")
                } else if(it instanceof PasswordCallback) {
                    PasswordCallback callback = (PasswordCallback)it
                    callback.password = System.console().readPassword(callback.prompt+" ")
                }
            }
        }
    }
}
