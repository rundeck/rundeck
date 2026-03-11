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

import rundeckapp.init.RundeckInitConfig
import spock.lang.Specification



class CommandLineSetupTest extends Specification {

    ByteArrayOutputStream sysOut = new ByteArrayOutputStream()
    PrintStream pout = new PrintStream(sysOut)
    ByteArrayOutputStream sysErr = new ByteArrayOutputStream()
    PrintStream perr = new PrintStream(sysErr)

    def setup() {
        System.out = pout
        System.err = perr

    }

    def cleanup() {
        System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR)
        System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_LOG_DIR)
    }

    def "EncryptPassword fails without specifying service"() {
        when:
        int result=-1
        CommandLineSetup setup = new CommandLineSetup({result=it})

        setup.runSetup("--encryptpwd")

        then:
        result==1
        sysErr.toString().contains("Missing required parameter for option '--encryptpwd'")
    }

    def "EncryptPassword fails specifying unknown service"() {
        when:
        int result=-1
        CommandLineSetup setup = new CommandLineSetup({result=it})


        setup.runSetup("--encryptpwd", "Unknown")


        then:
        result==1
        sysErr.toString().contains("No encryption service named: Unknown")
    }

    def "EncryptPassword with Jetty"() {
        when:

        int result=-1
        CommandLineSetup setup = new CommandLineSetup({result=it})
        setup.console=Mock(CommandLineSetup.ConsoleI){
            2 * readLine() >>> [ "username","thepassword"]
        }
        setup.runSetup("--encryptpwd", "Jetty")

        def output = sysOut.toString().split('\\n').toList()

        then:
        result==0
        output.find { it.startsWith("==ENCRYPTED OUTPUT==") }
        output.find { it.startsWith("bcrypt:") }
        output.find { it.startsWith("obfuscate:") }
        output.find { it.startsWith("crypt:") }
        output.find { it.startsWith("md5:") }

    }

    def "EncryptPassword with Hidden Input"() {
        when:
        int result=-1
        CommandLineSetup setup = new CommandLineSetup({result=it})
        setup.console=Mock(CommandLineSetup.ConsoleI){
            1 * readLine() >> "username"
            _ * readPassword() >>  "thepassword".toCharArray()
        }
        setup.runSetup("--encryptpwd", "Hidden Input")

        def output = sysOut.toString().split('\\n').toList()

        then:
        result==0
        output.find { it.startsWith("==ENCRYPTED OUTPUT==") }
        output.find { it.startsWith("bcrypt:") }
        output.find { it.startsWith("obfuscate:") }
        output.find { it.startsWith("crypt:") }
        output.find { it.startsWith("md5:") }

    }

    def "Ensure cli option -c sets config directory"() {
        when:

        int result=-1
        CommandLineSetup setup = new CommandLineSetup({result=it})
        RundeckCliOptions opts = setup.runSetup("-c","/tmp/config")

        then:
        opts.configDir == "/tmp/config"
    }

    def "Test cli options"() {
        setup:
        System.clearProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR)
        when:

        int result=-1
        CommandLineSetup setup = new CommandLineSetup({result=it})
        RundeckCliOptions opts = setup.runSetup("-b","/tmp/base")

        then:
        opts.baseDir == "/tmp/base"
        opts.serverBaseDir == "/tmp/base/server"
        opts.configDir == "/tmp/base/server/config"
        opts.logDir == "/tmp/base/server/logs"
        opts.dataDir == "/tmp/base/server/data"
    }
}
