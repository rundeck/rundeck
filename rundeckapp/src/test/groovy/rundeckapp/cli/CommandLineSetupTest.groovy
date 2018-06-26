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
package rundeckapp.cli

import spock.lang.Specification

import java.security.Permission


class CommandLineSetupTest extends Specification {

    ByteArrayOutputStream sysOut = new ByteArrayOutputStream()
    PrintStream pout = new PrintStream(sysOut)
    ByteArrayOutputStream sysErr = new ByteArrayOutputStream()
    PrintStream perr = new PrintStream(sysErr)

    def setup() {
        System.out = pout
        System.err = perr
        System.metaClass.static.exit = { int status ->
            throw new Exception("system.exit "+status)
        }
    }

    def "EncryptPassword fails without specifying service"() {
        when:
        CommandLineSetup setup = new CommandLineSetup()
        Exception ex
        try {
            setup.runSetup("--encryptpwd")
        } catch(Exception tex) {
            ex = tex
        }

        then:
        ex.message == "system.exit 1"
        sysErr.toString() == "Parsing failed.  Reason: no argument for: \n"
    }

    def "EncryptPassword fails specifying unknown service"() {
        when:
        CommandLineSetup setup = new CommandLineSetup()
        Exception ex
        try {
            setup.runSetup("--encryptpwd", "Unknown")
        } catch(Exception tex) {
            ex = tex
        }

        then:
        ex.message == "system.exit 1"
        sysErr.toString() == "No encryption service named: Unknown\n"
    }

    def "EncryptPassword with Jetty"() {
        when:
        int linesWritten = 0
        def console = new Console()
        console.metaClass.readLine = { ->
            if(linesWritten++ == 0) return "username\n"
            if(linesWritten++ == 1) return "thepassword\n"
            return "\n"
        }
        System.metaClass.static.console = { -> console }
        CommandLineSetup setup = new CommandLineSetup()
        Exception ex
        try {
            setup.runSetup("--encryptpwd", "Jetty")
        } catch(Exception tex) {
            ex = tex
        }

        def output = sysOut.toString().split('\\n')

        then:
        ex.message == "system.exit 0"
        output[output.length -4 ] == "==ENCRYPTED OUTPUT=="
        output[output.length -3 ].startsWith("obfuscate:")
        output[output.length -2 ].startsWith("md5:")
        output[output.length -1 ].startsWith("crypt:")

    }
}
