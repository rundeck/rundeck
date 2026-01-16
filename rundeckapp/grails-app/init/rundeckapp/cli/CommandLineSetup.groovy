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

import grails.util.Environment
import groovy.transform.CompileStatic
import com.dtolabs.rundeck.core.encrypter.PasswordUtilityEncrypter
import rundeckapp.Application
import rundeckapp.init.RundeckInitConfig
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option

import java.util.function.IntConsumer

/**
 * CommandLineSetup parses and holds command-line options for Rundeck / Runbook Automation server startup.
 */
@CompileStatic
@Command(
    name = 'rundeck-server',
    mixinStandardHelpOptions = true,
    description = [
        'Run the Rundeck / Runbook Automation server, installing the necessary components if they do not exist.'
    ]
)
class CommandLineSetup implements Runnable {
    private static final String TOOL_ROOT_DIR = "tools"
    //system props for launcher config
    private static final String SYS_PROP_RUNDECK_LAUNCHER_DEBUG     = "rundeck.launcher.debug"
    private static final String SYS_PROP_RUNDECK_LAUNCHER_REWRITE   = "rundeck.launcher.rewrite"
    public static final String SYS_PROP_MIGRATE_ONLY               = "migrate.only"

    public static final String FLAG_INSTALLONLY = "installonly"
    public static final String FLAG_SKIPINSTALL = "skipinstall"

    private static final Map<String,PasswordUtilityEncrypter> encrypters = getEncrypters()

    @Option(names = ['-b', '--basedir'], description = 'The basedir', paramLabel = 'PATH')
    String baseDir

    @Option(names = ['--serverdir'], description = 'The base directory for the server', paramLabel = 'PATH')
    String serverBaseDir

    @Option(names = ['-x', '--bindir'], description = 'The install directory for the tools used by users.', paramLabel = 'PATH')
    String binDir

    @Option(names = ['-s', '--sbindir'], description = 'The install directory for the tools used by administrators.', paramLabel = 'PATH')
    String sbinDir

    @Option(names = ['-c', '--configdir'], description = 'The location of the configuration.', paramLabel = 'PATH')
    String configDir

    @Option(names = ['--datadir'], description = 'The location of Rundeck\'s runtime data.', paramLabel = 'PATH')
    String dataDir

    @Option(names = ['-p', '--projectdir'], description = 'The location of Rundeck\'s project data.', paramLabel = 'PATH')
    String projectDir

    @Option(names = ['-h', '--help'], usageHelp = true, description = 'Display this message.')
    boolean helpRequested

    @Option(names = ['-d'], description = 'Show debug information')
    boolean debug

    @Option(names = ['--skipinstall'], description = 'Skip the extraction of the utilities from the launcher.')
    boolean skipInstall

    @Option(names = ['--installonly'], description = 'Perform installation only and do not start the server.')
    boolean installOnly

    @Option(names = ['--testauth'], description = 'Test Jaas authentication configuration.')
    boolean testAuth

    @Option(names = ['--encryptpwd'], description = 'Encrypt a password for use in a property file using the specified service. Available services: ${COMPLETION-CANDIDATES}', paramLabel = 'ENCRYPTION-SERVICE')
    String encryptPwdService

    @Option(names = ['-r', '--rollback'], description = 'Down migrate or rollback to previous db versions.', paramLabel = 'TAGNAME')
    String tag

    @Option(names = ['-m', '--migrate-only'], description = 'Run database migrations then exit.')
    boolean migrate

    private static final String USAGE_HEADER = "\nRun the rundeck server, installing the " +
                                                                               "necessary components if they do not exist.\n"
    private static final String USAGE_FOOTER = "\nhttp://rundeck.org\n"

    void run() {
        // This method is required by picocli, but not used directly.
    }
    IntConsumer exitHandler = System::exit

    CommandLineSetup() {
    }

    CommandLineSetup(final IntConsumer exitHandler) {
        this.exitHandler = exitHandler
    }

    RundeckCliOptions runSetup(String[] args) {
        // Use picocli to parse args and populate this instance
        CommandLine cmd = new CommandLine(this)
        try {
            cmd.parseArgs(args)
        } catch (CommandLine.ParameterException ex) {
            System.err.println(ex.getMessage())
            cmd.usage(System.err)
            exitHandler.accept(1)
            return
        }
        if (helpRequested) {
            cmd.usage(System.out)
            exitHandler.accept(0)
            return
        }
        if (installOnly && skipInstall) {
            System.err.println("--installonly and --skipinstall are mutually exclusive")
            cmd.usage(System.err)
            exitHandler.accept(1)
            return
        }
        if (encryptPwdService) {
            if(encryptPassword(encryptPwdService)) {
                exitHandler.accept(0)
            }
            return
        }
        RundeckCliOptions cliOptions = new RundeckCliOptions()
        cliOptions.debug = debug || Boolean.getBoolean(SYS_PROP_RUNDECK_LAUNCHER_DEBUG)
        cliOptions.rewrite = Boolean.getBoolean(SYS_PROP_RUNDECK_LAUNCHER_REWRITE)
        cliOptions.baseDir = baseDir ?: System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR, getLaunchLocationParentDir())
        cliOptions.serverBaseDir = serverBaseDir ?: System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_SERVER_DIR, cliOptions.baseDir+ "/server")
        cliOptions.logDir = System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_SERVER_DIR, cliOptions.serverBaseDir+ "/logs")
        cliOptions.configDir = configDir ?: System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR, cliOptions.serverBaseDir + "/config")
        cliOptions.dataDir = dataDir ?: System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_WORK_DIR, cliOptions.serverBaseDir + "/data")
        cliOptions.binDir = binDir ?: cliOptions.baseDir+"/"+TOOL_ROOT_DIR + "/bin"
        cliOptions.projectDir = projectDir
        cliOptions.skipInstall = skipInstall
        cliOptions.installOnly = installOnly
        cliOptions.testAuth = testAuth
        cliOptions.tag = tag ?: ""
        if(!System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR) && cliOptions.baseDir) {
            System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR, cliOptions.baseDir)
        }
        if(!System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR) && cliOptions.configDir) {
            System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR, cliOptions.configDir)
        }
        if(!System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_LOG_DIR) && cliOptions.logDir) {
            System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_LOG_DIR, cliOptions.logDir)
        }
        if(!System.getProperty("logging.config")) {
            System.setProperty("logging.config",System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR)+"/log4j2.properties")
        }
        if(tag) cliOptions.rollback = true
        cliOptions.migrate = migrate
        if(cliOptions.migrate) {
            System.setProperty(SYS_PROP_MIGRATE_ONLY, "true")
        }
        return cliOptions
    }

    //This happens when no RDECK_DIR was specified so we have to make a sane default
    String getLaunchLocationParentDir() {
        if(Environment.current == Environment.DEVELOPMENT) {
            File baseRundeckDir = new File(System.getProperty("user.dir"),"rundeck-runtime")
            if(!baseRundeckDir.exists()) baseRundeckDir.mkdirs()
            return baseRundeckDir.absolutePath
        } else {
            try {
                //spring boot launched jar scenario
                //The location the jar is launched from is assumed to be the desired root
                Class baseClass = ClassLoader.getSystemClassLoader().loadClass("org.springframework.boot.loader.Launcher")
                return new File(baseClass.getProtectionDomain().getCodeSource().getLocation().getFile()).getParentFile().absolutePath
            } catch(Exception ex) {}
            //War file scenario, the exploded class should be in WEB-INF/classes which is why we back up two directories then create the root
            File rundeckParent = new File(Application.class.getProtectionDomain().getCodeSource().getLocation().getFile()).parentFile.parentFile
            File rundeckRoot = new File(rundeckParent,"rundeck")
            rundeckRoot.mkdirs()
            rundeckRoot.absolutePath
        }

    }
    static interface ConsoleI{
        String readLine()
        char[] readPassword()
    }
    ConsoleI console = System.console() as ConsoleI

    boolean encryptPassword(String service) {
        PasswordUtilityEncrypter encrypter = encrypters[service.toUpperCase()]
        if(!encrypter) {
            System.err.println("No encryption service named: ${service}")
            exitHandler.accept(1)
            return false
        }
        Map input = [:]
        String rqMarker = "*"
        System.out.println("Required values are marked with: ${rqMarker} ")
        encrypter.formProperties().each { prop ->
            System.out.println((prop.isRequired() ? rqMarker : "") + prop.title + " (${prop.description}):")
            String val = ""
            if(prop.renderingOptions["displayType"] == "PASSWORD") val = new String(console.readPassword())
            else val = console.readLine()
            if(prop.isRequired() && val.isEmpty()) {
                System.out.println("${prop.title} is required.")
                exitHandler.accept(1)
                return false
            }
            input[prop.name] = val
        }
        System.out.println("\n==ENCRYPTED OUTPUT==")
        encrypter.encrypt(input).each { k, v ->
            System.out.println("${k}: ${v}")
        }
        true
    }

    static Map<String,PasswordUtilityEncrypter> getEncrypters() {
        Map<String,PasswordUtilityEncrypter> encrypters= [:]
        ServiceLoader<PasswordUtilityEncrypter> encrypterServices = ServiceLoader.load(
                PasswordUtilityEncrypter
        )
        encrypterServices.each { encrypters[it.name().toUpperCase()] = it }
        return encrypters
    };
}
