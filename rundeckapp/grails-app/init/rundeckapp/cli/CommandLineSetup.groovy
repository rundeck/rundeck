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
import org.apache.commons.cli.CommandLine
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.GnuParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Option
import org.apache.commons.cli.OptionBuilder
import org.apache.commons.cli.Options
import org.apache.commons.cli.ParseException
import rundeckapp.Application
import rundeckapp.init.RundeckInitConfig


class CommandLineSetup {
    private static final String TOOL_ROOT_DIR = "tools"
    //system props for launcher config
    private static final String SYS_PROP_RUNDECK_LAUNCHER_DEBUG     = "rundeck.launcher.debug";
    private static final String SYS_PROP_RUNDECK_LAUNCHER_REWRITE   = "rundeck.launcher.rewrite";

    public static final String FLAG_INSTALLONLY = "installonly";
    public static final String FLAG_SKIPINSTALL = "skipinstall";

    private final Options options = new Options();

    CommandLineSetup() {
        Option baseDir =    OptionBuilder.withLongOpt("basedir")
                                         .hasArg()
                                         .withDescription("The basedir")
                                         .withArgName("PATH")
                                         .create('b');

        Option serverDir =  OptionBuilder.withLongOpt("serverdir")
                                         .hasArg()
                                         .withDescription("The base directory for the server")
                                         .withArgName("PATH")
                                         .create();

        Option binDir =     OptionBuilder.withLongOpt("bindir")
                                         .hasArg()
                                         .withArgName("PATH")
                                         .withDescription("The install directory for the tools used by users.")
                                         .create('x');

        Option sbinDir =    OptionBuilder.withLongOpt("sbindir")
                                         .hasArg()
                                         .withArgName("PATH")
                                         .withDescription("The install directory for the tools used by administrators.")
                                         .create('s');

        Option configDir =  OptionBuilder.withLongOpt("configdir")
                                         .hasArg()
                                         .withArgName("PATH")
                                         .withDescription("The location of the configuration.")
                                         .create('c');

        Option dataDir =    OptionBuilder.withLongOpt("datadir")
                                         .hasArg()
                                         .withArgName("PATH")
                                         .withDescription("The location of Rundeck's runtime data.")
                                         .create();

        Option projectDir =    OptionBuilder.withLongOpt("projectdir")
                                            .hasArg()
                                            .withArgName("PATH")
                                            .withDescription("The location of Rundeck's project data.")
                                            .create('p');

        Option help =       OptionBuilder.withLongOpt("help")
                                         .withDescription("Display this message.")
                                         .create('h');

        Option debugFlag =  OptionBuilder.withDescription("Show debug information")
                                         .create('d');

        Option skipInstall = OptionBuilder.withLongOpt(FLAG_SKIPINSTALL)
                                          .withDescription("Skip the extraction of the utilities from the launcher.")
                                          .create();

        Option installonly = OptionBuilder.withLongOpt(FLAG_INSTALLONLY)
                                          .withDescription("Perform installation only and do not start the server.")
                                          .create();

        options.addOption(baseDir);
        options.addOption(dataDir);
        options.addOption(serverDir);
        options.addOption(binDir);
        options.addOption(sbinDir);
        options.addOption(configDir);
        options.addOption(help);
        options.addOption(debugFlag);
        options.addOption(skipInstall);
        options.addOption(installonly);
        options.addOption(projectDir);
    }

    RundeckCliOptions runSetup(String[] args) {
        RundeckCliOptions cliOptions = new RundeckCliOptions()
        cliOptions.debug = Boolean.getBoolean(SYS_PROP_RUNDECK_LAUNCHER_DEBUG);
        cliOptions.rewrite = Boolean.getBoolean(SYS_PROP_RUNDECK_LAUNCHER_REWRITE);

        final CommandLineParser parser = new GnuParser();

        final CommandLine cl;
        try {
            cl = parser.parse(this.options, args);

            if(cl.hasOption('h')) {
                printUsage();
                System.exit(0);
            }
            if(cl.hasOption(FLAG_INSTALLONLY) && cl.hasOption(FLAG_SKIPINSTALL)) {
                ERR("--" + FLAG_INSTALLONLY + " and --" + FLAG_SKIPINSTALL + " are mutually exclusive");
                printUsage();
                System.exit(1);
            }

        } catch (ParseException e) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
            System.exit(1);
        }
        cliOptions.debug = cl.hasOption('d');

        cliOptions.baseDir = cl.getOptionValue('b', System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR, getLaunchLocationParentDir()))
        cliOptions.serverBaseDir = cl.getOptionValue("serverdir", System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_SERVER_DIR, cliOptions.baseDir+ "/server"))
        cliOptions.configDir = cl.getOptionValue("c", System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR, cliOptions.serverBaseDir + "/config"))
        cliOptions.dataDir = cl.getOptionValue("datadir", System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_WORK_DIR, cliOptions.serverBaseDir + "/data"))
        cliOptions.binDir = cl.getOptionValue('x', cliOptions.baseDir+"/"+TOOL_ROOT_DIR + "/bin")

        cliOptions.projectDir = cl.getOptionValue('p')
        cliOptions.skipInstall = cl.hasOption(FLAG_SKIPINSTALL)
        cliOptions.installOnly = cl.hasOption(FLAG_INSTALLONLY)

        if(!System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR) && cliOptions.baseDir) {
            System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR, cliOptions.baseDir)
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

    Class tryToLoadCorrectBaseClass() {
        //For spring boot launched jar this will be correct
        try {
            return ClassLoader.getSystemClassLoader().loadClass("org.springframework.boot.loader.Launcher")
        } catch(Exception ex) {}
        //Otherwise use the regular class loader and use any class in it
        return Application.class
    }

    private void printUsage() {
        // automatically generate the help statement
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "java [JAVA_OPTIONS] -jar rundeck-launcher.war ", "\nRun the rundeck server, installing the " +
                                                                               "necessary components if they do not exist.\n", options,
                             "\nhttp://rundeck.org\n", true );
    }

    /**
     * Print err message
     *
     * @param s
     */
    private void ERR(final String s) {
        System.err.println("ERROR: " + s);
    }

}
