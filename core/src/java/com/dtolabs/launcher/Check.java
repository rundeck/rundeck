/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* Check.java
* 
* User: greg
* Created: Oct 7, 2009 10:57:02 AM
* $Id$
*/
package com.dtolabs.launcher;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.utils.PropertyUtil;
import com.dtolabs.launcher.check.PolicyAnalyzer;
import com.dtolabs.launcher.check.PolicyAnalyzerImpl;
import com.dtolabs.launcher.check.PolicyAnalyzerListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;

/**
 * Check performs correctness checks on the contents of a RDECK_BASE directory, printing warnings for incorrect or missing
 * contents.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * @version $Revision$
 */
public class Check implements PolicyAnalyzerListener {
    /**
     * basic bootstrapped rdeck.home
     */
    public static String RDECK_HOME = Constants.getSystemHomeDir();
    /**
     * check usage
     */
    public static final String CHECK_USAGE =
        "rd-check [-vq] -d basedir  -n nodename -N hostname -s serverhostname [ --key=value ]";
    /**
     * location of the to be generated preferences.properties file
     */
//    public static File frameworkPreferences = new File(Constants.getFrameworkPreferences(RDECK_BASE));


    private RequiredParams requiredParams;
    private Properties inputProperties;
    private boolean invalid;
    private Reporter reporter = null;
    private PolicyAnalyzer policy = null;
    private Properties ctlDefaultProperties ;

    /**
     * default constructor
     */
    public Check() {
        requiredParams = new RequiredParams();
        inputProperties = new Properties();
        this.reporter = new MainReporter();
        this.policy = new PolicyAnalyzerImpl(this);
        ctlDefaultProperties=new Properties();
        try {
            Preferences.loadResourcesDefaults(ctlDefaultProperties, Preferences.RUNDECK_DEFAULTS_PROPERTIES_NAME);
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Unable to load default properties file: " + e.getMessage());
        }

        ctlDefaultProperties.setProperty("rdeck.home", RDECK_HOME);
    }


    /**
     * called from setup shell/bat script. Calls the {@link #performCheck()} method.
     * @param args arguments
     */
    public static void main(final String args[]) {
        int exitCode = 2;
        final Check check = new Check();
        try {
            final boolean parsed = check.parseArgs(args);
            if(parsed){
                final boolean valid = check.performCheck();
                exitCode = valid ? 0 : 1;
            }
        } catch (Throwable exc) {
            System.err.println("ERROR: " + exc.getMessage());
            if (check.requiredParams.isDebugFlag()) {
                exc.printStackTrace();
            }
        }
        System.exit(exitCode);
    }

    boolean parseArgs(final String[] args) throws CheckException {


        final boolean okargs = requiredParams.parse(args);
        if (!okargs) {
            return okargs;
        }
        inputProperties.putAll(requiredParams.getProperties());
        try {
            Preferences.parseNonReqOptionsAsProperties(inputProperties, args);
        } catch (Exception e) {
            throw new CheckException(e.getMessage());
        }
        ctlDefaultProperties.putAll(System.getProperties());
        ctlDefaultProperties.putAll(inputProperties);
        ctlDefaultProperties=PropertyUtil.expand(ctlDefaultProperties);
        return true;
    }


    /**
     * Return true if the Ctl Base directory contains a valid installation.
     *
     * @return true if all contents are valid
     *
     * @throws CheckException thrown if error
     */
    public boolean performCheck() throws CheckException {
        validateInstall(requiredParams);
        testBaseDir(requiredParams, inputProperties);
        if (invalid) {
            reporter.reportError("NOT OK: RUNDECK Base directory check FAILED: " + requiredParams.getBaseDir(), true);
        } else if (!requiredParams.isQuietFlag()) {
            reporter.reportNominal("OK: RUNDECK Base directory check complete: " + requiredParams.getBaseDir());
        }
        return !invalid;
    }

    /**
     * Check the contents of the Base directory and print warnings/errors.
     *
     * @param params parameters
     * @param inputProps input properties
     *
     * @throws CheckException if the Base is missing or not a directory
     */
    void testBaseDir(final RequiredParams params, final Properties inputProps) throws CheckException {
        final File baseDir = new File(params.getBaseDir());
        if (!baseDir.isDirectory()) {
            throw new CheckException("basedir path is not a directory: " + params.getBaseDir());
        }

        testBaseDirectories(baseDir);
        testBaseFiles(baseDir);
        testEtcFrameworkProperties(baseDir, params, inputProps);
        testEtcDepotProperties(baseDir, inputProps);
        testEtcLog4jProperties(baseDir, inputProps);
//        testEtcNodeProperties(Base, inputProps);
    }

    /**
     * test existence of files
     *
     * @param baseDir RUNDECK base directory
     */
    void testBaseFiles(final File baseDir) {
        final File etcDir = new File(Constants.getFrameworkConfigDir(baseDir.getAbsolutePath()));
        reporter.beginCheckOnDirectory(etcDir);
        final String[] requiredFiles = new String[]{
            "framework.properties",
            "project.properties",
            "log4j.properties",
//            "node.properties",
            "preferences.properties",
            "profile"
        };

        if (!testDirFilesExist(etcDir, Arrays.asList(requiredFiles), false, true)) {
            reporter.incorrectDirectory(etcDir, true);
        } else {
            reporter.expectedDirectory(etcDir);
        }

    }

    boolean testDirFilesExist(final File dir, final Collection<String> requiredFiles, final boolean directory, final boolean required) {
        boolean success = true;
        for (final String file : requiredFiles) {
            final File testFile = new File(dir, file);

            if (required && !policy.requireFileExists(testFile, directory)) {
                success = false;
            } else if (!required && !policy.expectFileExists(testFile, directory)) {
                success = false;
            }
        }
        return success;
    }   

    /**
     * Check existence of required directories
     *
     * @param baseDir RUNDECK base directory
     */
    void testBaseDirectories(final File baseDir) {
        final String[] reqDirs = new String[]{
            "etc",
            "projects",
            "var"
        };
        policy.expectFileExists(new File(baseDir, "src"), true);
        final File varDir = new File(baseDir, "var");
        policy.expectFileExists(new File(varDir, "logs"), true);
        policy.expectFileExists(new File(varDir, "tmp"), true);

        if (!testDirFilesExist(baseDir, Arrays.asList(reqDirs), true, true)) {
            reporter.incorrectDirectory(baseDir, true);
        }
    }


    void testEtcDepotProperties(final File baseDir, final Properties inputProps) throws
        CheckException {
        final File file = new File(baseDir, "etc/project.properties");
//        Properties props = PropertyUtil.expand(fileprops);

        final Properties requiredProps = new Properties();
        requiredProps.setProperty("project.dir", "${framework.projects.dir}/${project.name}");
        requiredProps.setProperty("project.etc.dir", "${project.dir}/etc");
        requiredProps.setProperty("project.resources.file", "${project.etc.dir}/resources.xml");

        final ArrayList<String> expectedKeys = new ArrayList<String>();
        expectedKeys.add("project.description");
        expectedKeys.add("project.default.create");
        expectedKeys.add("project.organization");
        expectedKeys.add("project.default.name");
        reporter.beginCheckOnProperties(file);
        testPropertiesFile(file, expectedKeys, null, false, false, inputProps);
        testPropertiesFile(file, null, requiredProps, false, true, inputProps);
    }


    void testEtcLog4jProperties(final File baseDir, final Properties inputProps) throws
        CheckException {
        final File file = new File(new File(Constants.getFrameworkConfigDir(baseDir.getAbsolutePath())), "log4j.properties");

        final Properties expectedProps = new Properties();
        /*
        log4j.logger.com.dtolabs.log.common=INFO,CommonLog
        log4j.appender.CommonLog=org.apache.log4j.net.SocketAppender
log4j.appender.CommonLog.remoteHost=@framework.server.hostname@
log4j.appender.CommonLog.port=@reportcenter.log4j.port@
log4j.appender.CommonLog.locationInfo=true
         */
        expectedProps.setProperty("log4j.logger.com.dtolabs.rundeck.log.common", "INFO,CommonLog");
        expectedProps.setProperty("log4j.appender.CommonLog", "org.apache.log4j.net.SocketAppender");
        expectedProps.setProperty("log4j.appender.CommonLog.remoteHost", requiredParams.getServerHostname());
        expectedProps.setProperty("log4j.appender.CommonLog.port", "1055");
        expectedProps.setProperty("log4j.appender.CommonLog.locationInfo", "true");


        expectedProps.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        expectedProps.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        expectedProps.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%-5p %c{1}: %m%n");

        expectedProps.setProperty("log4j.appender.plainStdout", "org.apache.log4j.ConsoleAppender");
        expectedProps.setProperty("log4j.appender.plainStdout.layout", "org.apache.log4j.PatternLayout");
        expectedProps.setProperty("log4j.appender.stdout.plainStdout.ConversionPattern", "%m%n");


        expectedProps.setProperty("log4j.appender.file", "org.apache.log4j.DailyRollingFileAppender");
        expectedProps.setProperty("log4j.appender.file.file", baseDir.getAbsolutePath() + "/var/logs/command.log");
        expectedProps.setProperty("log4j.appender.file.datePattern", "'.'yyyy-MM-dd");
        expectedProps.setProperty("log4j.appender.file.append", "true");
        expectedProps.setProperty("log4j.appender.file.layout", "org.apache.log4j.PatternLayout");
        expectedProps.setProperty("log4j.appender.file.layout.ConversionPattern", "%d{ISO8601} [%t] %-5p %c - %m%n");

        expectedProps.setProperty("log4j.appender.Chainsaw", "org.apache.log4j.net.SocketAppender");
        expectedProps.setProperty("log4j.appender.Chainsaw.remoteHost", "localhost");
        expectedProps.setProperty("log4j.appender.Chainsaw.port", "4445");
        expectedProps.setProperty("log4j.appender.Chainsaw.locationInfo", "true");


        reporter.beginCheckOnProperties(file);
        testPropertiesFile(file, null, expectedProps, false, false, inputProps);
    }

    void testEtcNodeProperties(final File baseDir, final Properties inputProps) throws CheckException {
        final File file = new File(baseDir, "etc/node.properties");

        /**
         * node.description=
         node.type=Node
         node.name=gozer
         node.hostname=gozer
         node.os-arch=x86_64
         node.os-family=unix
         node.os-name=Mac OS X
         node.os-version=10.5.8
         */

        final Properties requiredProps = new Properties();
        final Properties expectedProps = new Properties();
        final ArrayList<String> expectedKeys = new ArrayList<String>();

        requiredProps.setProperty("node.type", "Node");
        requiredProps.setProperty("node.name", requiredParams.getNodeName());
        requiredProps.setProperty("node.hostname", requiredParams.getNodeHostname());

        expectedProps.setProperty("node.os-arch", System.getProperty("os.arch"));
        final String osfam = System.getProperty("os.family");
        if (null != osfam) {
            expectedProps.setProperty("node.os-family", osfam);
        } else {
            expectedKeys.add("node.os-family");
        }
        expectedProps.setProperty("node.os-name", System.getProperty("os.name"));
        expectedProps.setProperty("node.os-version", System.getProperty("os.version"));
        expectedKeys.add("node.description");


        //set any input properties to override the defaults
        for (final Object o : inputProps.keySet()) {
            final String s = (String) o;
            if (requiredProps.containsKey(s)) {
                requiredProps.setProperty(s, inputProps.getProperty(s));
            } else if (expectedProps.containsKey(s)) {
                expectedProps.setProperty(s, inputProps.getProperty(s));
            }
        }

        reporter.beginCheckOnProperties(file);
        testPropertiesFile(file, expectedKeys, expectedProps, false, false, inputProps);
        testPropertiesFile(file, null, requiredProps, false, true, inputProps);
    }

    /**
     * load the framework.properties file and check values based on input parameters
     *
     * @param baseDir RUNDECK base directory
     * @param params input parameters
     * @param inputProps input properties
     * @throws com.dtolabs.launcher.Check.CheckException if exception occurs
     */
    void testEtcFrameworkProperties(final File baseDir, final RequiredParams params,
                                    final Properties inputProps) throws CheckException {
        final File file = new File(new File(Constants.getFrameworkConfigDir(baseDir.getAbsolutePath())), "framework.properties");
        final Collection<String> requiredExistProperties = new ArrayList<String>();


        final String[] requiredProps = {
            "java.home",
            "rdeck.home",
            "rdeck.base",
            "framework.rdeck.base",
            "framework.rdeck.dir",
            "framework.email.tolist",
            "framework.email.from",
            "framework.email.replyto",
            "framework.email.mailhost",
            "framework.email.mailport",
            "framework.email.user",
            "framework.email.password",
            "framework.email.ssl",
            "framework.email.failonerror",
            "framework.log.format",
            "framework.server.username",
            "framework.server.password",
            "framework.ssh.keypath",
            "framework.ssh.user",
            "framework.ssh.timeout",
            "framework.log.dispatch.console.format",
            "framework.server.url",
            "framework.rundeck.url",
            "framework.rdeck.version",
            "framework.authorization.class",
            "framework.authentication.class",
            "framework.centraldispatcher.classname",
            "framework.nodeauthentication.classname",
            "framework.projects.dir",
            "framework.etc.dir",
            "framework.var.dir",
            "framework.logs.dir",
            "framework.src.dir",
            "framework.nodes.file.name",
            "framework.server.hostname",
            "framework.node.name",
            "framework.node.hostname",
            "framework.node",
            "framework.node.type",

        };
        requiredExistProperties.addAll(Arrays.asList(requiredProps));

        final Properties reqdefaults = new Properties();
        reqdefaults.setProperty("rdeck.base", params.getBaseDir());
        reqdefaults.setProperty("framework.rdeck.base", params.getBaseDir());
        reqdefaults.setProperty("rdeck.home", RDECK_HOME);
        reqdefaults.setProperty("framework.rdeck.dir", RDECK_HOME);
        reqdefaults.setProperty("framework.node.type", "Node");

        if (null != params.getNodeName()) {
            reqdefaults.setProperty("framework.node.name", params.getNodeName());
            reqdefaults.setProperty("framework.node", params.getNodeName());
        }
        if (null != params.getNodeHostname()) {
            reqdefaults.setProperty("framework.node.hostname", params.getNodeHostname());
        }

        for (Object o : reqdefaults.keySet()) {
            if (ctlDefaultProperties.containsKey(o)) {
                reqdefaults.put(o, ctlDefaultProperties.get(o));
            }
        }
        for (String key : requiredExistProperties) {
            if (ctlDefaultProperties.containsKey(key)) {
                reqdefaults.put(key, ctlDefaultProperties.get(key));
            }
        }
        reqdefaults.setProperty("framework.nodeauthentication.classname",
            "com.dtolabs.rundeck.core.authentication.DefaultNodeAuthResolutionStrategy");
        reporter.beginCheckOnProperties(file);
        testPropertiesFile(file, requiredExistProperties, reqdefaults, true, true, inputProps);
    }

    /**
     * Test a properties file for required contents. Substitue input properties for expected properties if a corresponding property
     * exists in the expected properties.
     *
     * @param required         true if the values/keys are required
     * @param inputProps       input properties
     * @param file             the properties file path
     * @param existKeys        property keys that must exist
     * @param expectedProperties           properties that are expected to be set in the file
     * @param expandProperties if true, expand embedded properties in the file after loading it
     *
     * @throws com.dtolabs.launcher.Check.CheckException if exception occurs
     */
    void testPropertiesFile(final File file, final Collection<String> existKeys, final Properties expectedProperties,
                                    final boolean expandProperties,
                                    final boolean required, final Properties inputProps) throws CheckException {
        testPropertiesFile(file, existKeys, expectedProperties, expandProperties, required, inputProps, policy, reporter);
    }

    /**
     * Test a properties file for required contents. Substitue input properties for expected properties if a corresponding property
     * exists in the expected properties.
     *
     * @param required         true if the values/keys are required
     * @param inputProps       input properties
     * @param policy           the policy analyzer
     * @param reporter         the reporter to report results
     * @param file             the properties file path
     * @param existKeys        property keys that must exist
     * @param expectedProperties           properties that are expected to be set in the file
     * @param expandProperties if true, expand embedded properties in the file after loading it
     *
     * @throws CheckException if exception occurs
     */
    static void testPropertiesFile(final File file, final Collection<String> existKeys, final Properties expectedProperties,
                             final boolean expandProperties,
                             final boolean required, final Properties inputProps, final PolicyAnalyzer policy,
                             final Reporter reporter) throws CheckException {


        final Properties testProperties=new Properties();
        if(null!=expectedProperties && expectedProperties.size()>0) {
            testProperties.putAll(expectedProperties);
        }


        //set any input properties to override the expected values
        if (null != inputProps) {
            for (final Object o : inputProps.keySet()) {
                final String s = (String) o;
                if (testProperties.containsKey(s) || (null!=existKeys && existKeys.contains(s))) {
                    testProperties.setProperty(s, inputProps.getProperty(s));
                }
            }
        }

        Properties fileProperties = loadPropertiesFile(file);
        if (expandProperties) {
            fileProperties = PropertyUtil.expand(fileProperties);
        }

        int reqExistCount = 0;
        int reqExistTot = 0;
        if (null != existKeys && existKeys.size() > 0) {
            if (required) {
                reqExistCount = policy.requirePropertiesExist(existKeys, fileProperties);
            } else {
                reqExistCount = policy.expectPropertiesExist(existKeys, fileProperties);
            }
            reqExistTot = existKeys.size();
        }

        int reqValCount = 0;
        int reqValTot = 0;
        if (testProperties.size() > 0) {
            if (required) {
                reqValCount = policy.requirePropertyValues(testProperties, fileProperties);
            } else {
                reqValCount = policy.expectPropertyValues(testProperties, fileProperties);
            }
            reqValTot = testProperties.keySet().size();
        }

        final int reqFound = reqExistCount + reqValCount;
        final int reqTot = reqExistTot + reqValTot;
        reporter.reportNominal(reqFound + "/" + reqTot + (required ? " required" : "") + " properties OK");
        if (reqFound < reqTot) {
            reporter.incorrectFile(file, required);
        }
    }

    /**
     * Load properties file into Properties object.
     * @param file the file
     * @return the Properties
     * @throws CheckException if there is an exception loading it
     */
    static Properties loadPropertiesFile(final File file) throws CheckException {
        final Properties props = new Properties();
        try {
            props.load(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new CheckException("Unable to load properties file: " + file.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
            throw new CheckException("Unable to load properties file: " + file.getAbsolutePath());
        }
        return props;
    }

    /**
     * Checks the basic install assumptions
     *
     * @param params parameters
     *
     * @throws com.dtolabs.launcher.Check.CheckException if RDECK_HOME or -d basedir option are invalid
     */
    void validateInstall(final RequiredParams params) throws CheckException {
        if (!checkIfDir("rdeck.home", RDECK_HOME)) {
            throw new CheckException(RDECK_HOME + " is not a valid rdeck install");
        }

        // check if rdeck.base is defined.
        if (!checkIfDir("-d basedir", params.getBaseDir())) {
            throw new CheckException("-d basedir option is missing or not a directory");
        }

    }

    // check if path exists as a directory
    boolean checkIfDir(final String propName, final String path) {
        if (null == path || path.equals("")) {
            throw new IllegalArgumentException(propName + "property had null or empty value");
        }
        return new File(path).exists();
    }


    /**
     * echo usage
     */
    public static void printUsage() {
        System.err.println(CHECK_USAGE);
    }

    /**
     * set Reporter
     * @param reporter reporter
     */
    public void setReporter(final Reporter reporter) {
        this.reporter = reporter;
    }

    /**
     * get parameters
     * @return parameters
     */
    public RequiredParams getRequiredParams() {
        return requiredParams;
    }

    /**
     * set params
     * @param requiredParams params
     */
    public void setRequiredParams(RequiredParams requiredParams) {
        this.requiredParams = requiredParams;
    }

    /**
     * get input properties
     * @return properties
     */
    public Properties getInputProperties() {
        return inputProperties;
    }

    /**
     * set input properties
     * @param inputProperties properties
     */
    public void setInputProperties(Properties inputProperties) {
        this.inputProperties = inputProperties;
    }

    public boolean isInvalid() {
        return invalid;
    }


    public Reporter getReporter() {
        return reporter;
    }

    private void setInvalid(boolean invalid) {
        this.invalid = invalid;
    }

    public void beginCheckOnFile(File file) {
        reporter.beginCheckOnFile(file);
    }

    public void beginCheckOnDirectory(File file) {
        reporter.beginCheckOnDirectory(file);
    }

    public void beginCheckOnProperties(File file) {
        reporter.beginCheckOnProperties(file);
    }

    public void passedFile(File file) {
        reporter.expectedFile(file);
    }

    public void failedFile(File file, boolean missing, boolean incorrectType, boolean invalidated) {
        if (invalidated) {
            setInvalid(true);
        }
        if (missing) {
            reporter.missingFile(file, invalidated);
        } else if (incorrectType) {
            reporter.notAFile(file, invalidated);
        } else {
            reporter.incorrectFile(file, invalidated);
        }
    }

    public void passedDirectory(File file) {
        reporter.expectedDirectory(file);
    }

    public void failedDirectory(File file, boolean missing, boolean incorrectType, boolean invalidated) {
        if (invalidated) {
            setInvalid(true);
        }
        if (missing) {
            reporter.missingDirectory(file, invalidated);
        } else if (incorrectType) {
            reporter.notADirectory(file, invalidated);
        } else {
            reporter.incorrectDirectory(file, invalidated);
        }
    }

    public void failedPropertyValue(String key, String value, String expected, boolean invalidated) {
        if (invalidated) {
            setInvalid(true);
        }
        reporter.incorrectPropertyValue(key, value, expected, invalidated);
    }

    public void passedPropertyValue(String key, String value) {
        reporter.expectedPropertyValue(key, value);
    }

    class MainReporter implements Check.Reporter {

        public void incorrectFile(File file, boolean invalidated) {
            reportError("file had " + (invalidated ? "errors" : "warnings") + ": " + file.getAbsolutePath(),
                invalidated);
        }

        public void incorrectDirectory(File file, boolean invalidated) {
            reportError("directory had " + (invalidated ? "errors" : "warnings") + ": " + file.getAbsolutePath(), invalidated);
        }

        public void error(String message) {
            System.err.println(message);
        }

        public void warn(String message) {
            System.err.println(message);
        }

        public void log(String message) {
            System.out.println(message);
        }

        public void expectedFile(File file) {
            //no echo
//            reportNominal("[F] " + file.getAbsolutePath());
        }

        public void expectedDirectory(File file) {
            //no echo
//            reportNominal("[D] " + file.getAbsolutePath());
        }

        public void expectedPropertyValue(String key, String value) {
            //noop
        }

        public void missingFile(File file, boolean invalidated) {
            reportMissing("missing file: " + file.getAbsolutePath(), invalidated);
        }

        public void missingDirectory(File file, boolean invalidated) {
            reportMissing("missing directory: " + file.getAbsolutePath(), invalidated);
        }

        public void notAFile(File file, boolean invalidated) {
            reportError("not a file: " + file.getAbsolutePath(), invalidated);
        }

        public void notADirectory(File file, boolean invalidated) {
            reportError("not a directory: " + file.getAbsolutePath(), invalidated);
        }

        public void incorrectPropertyValue(String key, String value, String expected, boolean invalidated) {
            if(null==expected) {
                missingPropertyValue(key, expected, invalidated);
            }else{
                reportError("incorrect property: " + key + "=" + value + " [expected=" + expected + "]", invalidated);
            }
        }

        public void missingPropertyValue(String key, String expected, boolean invalidated) {
            reportError("missing property: " + key, invalidated);
        }

        public void beginCheckOnFile(File file) {
            reportNominal("Checking file: " + file.getAbsolutePath());
        }

        public void beginCheckOnDirectory(File file) {
            //no echo
//            reportNominal("Checking Directory: " + file.getAbsolutePath());
        }

        public void beginCheckOnProperties(File file) {
            reportNominal("Checking properties: " + file.getAbsolutePath());
        }

        public void reportError(String s, boolean invalidated) {
            if (!requiredParams.isQuietFlag() && (invalidated || requiredParams.isDebugFlag())) {
                if (invalidated) {
                    error((invalidated ? "! " : "* ") + s);
                } else {
                    warn((invalidated ? "! " : "* ") + s);
                }
            }
        }


        public void reportMissing(String s, boolean invalidated) {
            if (!requiredParams.isQuietFlag() && (invalidated || requiredParams.isDebugFlag())) {
                if (invalidated) {
                    error((invalidated ? "! " : "? ") + s);
                } else {
                    warn((invalidated ? "! " : "? ") + s);
                }
            }
        }

        public void reportNominal(String s) {
            if (!requiredParams.isQuietFlag() && requiredParams.isDebugFlag()) {
                log(". " + s);
            }
        }
    }

    public static interface Reporter {
        /**
         * log an error message
         *
         * @param message messsage
         */
        public void error(String message);

        /**
         * log a warning message
         *
         * @param message message
         */
        public void warn(String message);

        /**
         * log a message
         *
         * @param message message
         */
        public void log(String message);


        /**
         * Report check beginning on file
         * @param file the file
         */
        public void beginCheckOnFile(File file);

        /**
         * Report check beginning on directory
         * @param dir the dir
         */
        public void beginCheckOnDirectory(File dir);

        /**
         * Report properties check beginning on file
         * @param file file
         */
        public void beginCheckOnProperties(File file);

        /**
         * Report file exists as expected
         * @param file the file
         */
        public void expectedFile(File file);

        /**
         * Report dir exists as expected
         * @param file the file
         */
        public void expectedDirectory(File file);

        /**
         * Report property value exists as expected
         * @param key the property
         * @param value the expected value
         */
        public void expectedPropertyValue(String key, String value);

        /**
         * Report file is missing
         * @param file the file
         * @param invalidated if true, consequence is invalidation
         */
        public void missingFile(File file, boolean invalidated);

        /**
         * Report expected file is not a file
         * @param file the file
         * @param invalidated if true, consequence is invalidation
         */
        public void notAFile(File file, boolean invalidated);

        /**
         * Report file is not as expected
         * @param file the file
         * @param invalidated if true, consequence is invalidation
         */
        public void incorrectFile(File file, boolean invalidated);

        /**
         * Report directory is missing
         * @param dir the dir
         * @param invalidated if true, consequence is invalidation
         */
        public void missingDirectory(File dir, boolean invalidated);

        /**
         * Report directory is not a directory
         * @param dir the dir
         * @param invalidated if true, consequence is invalidation
         */
        public void notADirectory(File dir, boolean invalidated);

        /**
         * Report directory is not as expected
         * @param dir the directory
         * @param invalidated if true, consequence is invalidation
         */
        public void incorrectDirectory(File dir, boolean invalidated);

        /**
         * Report property value is not as expected
         * @param key the property
         * @param value the seen value
         * @param expected the expected value
         * @param invalidated if true, consequence is invalidation
         */
        public void incorrectPropertyValue(String key, String value, String expected, boolean invalidated);

        /**
         * Report property value is not present
         * @param key the property
         * @param expected the expected value
         * @param invalidated if true, consequence is invalidation
         */
        public void missingPropertyValue(String key, String expected, boolean invalidated);

        /**
         * log a nominal report message
         *
         * @param message message
         */
        void reportNominal(String message);

        /**
         * log a missing item report message
         *
         * @param message     message
         * @param invalidated true if cause of invalidation
         */
        void reportMissing(String message, boolean invalidated);

        /**
         * log an error report message
         *
         * @param message     message
         * @param invalidated true if cause of invalidation, false if warning
         */
        void reportError(String message, boolean invalidated);
    }


    /**
     * Inner class representing required parameters
     */
    protected static class RequiredParams {

        private boolean forceFlag=false;
        private boolean debugFlag=false;
        private boolean quietFlag=false;
        private String nodeName;
        private String nodeHostname;
        private String serverHostname;
        private String baseDir;

        protected boolean isForceFlag() {
            return forceFlag;
        }

        protected boolean isDebugFlag() {
            return debugFlag;
        }

        protected String getNodeName() {
            return nodeName;
        }

        /**
         * get parameter for an option
         *
         * @param args  command line arg vector
         * @param arg_i arg index
         *
         * @return arg value
         *
         * @throws CheckException thrown if param missing a required arg
         */
        String getOptParam(final String args[], final int arg_i) throws CheckException {
            if (arg_i == (args.length - 1)) {
                throw new CheckException("option: " + args[arg_i] + " must take a parameter");
            }

            if (args[arg_i + 1].startsWith("-")) {
                throw new CheckException(
                    "arg: " + args[arg_i] + " does not have a parameter, instead was provided: " + args[arg_i + 1]);
            }

            return args[arg_i + 1];
        }

        /**
         * process the required single hyphen parameters. support basic opt args:  forceFlag (-f), debugFlag (-v), and
         * nodeArg (-n <node>)
         *
         * @param args command line arg vector
         *
         * @throws CheckException thrown if missing required arg
         */
        boolean parse(final String args[]) throws CheckException {
            boolean accepted = true;
            StringBuffer sb = new StringBuffer();
            for (int i = 0 ; i < args.length ; i++) {
                if (args[i].equals("-N")) {
                    nodeHostname = getOptParam(args, i);
                    i++;
                } else if (args[i].equals("-d")) {
                    baseDir = getOptParam(args, i);
                    i++;
                } else if (args[i].equals("-n")) {
                    nodeName = getOptParam(args, i);
                    i++;
                } else if (args[i].equals("-s")) {
                    serverHostname = getOptParam(args, i);
                    i++;
                } else if (args[i].equals("-h")) {
                    printUsage();
                    return false;
                } else if (args[i].equals("-q")) {
                    quietFlag=true;
                } else if (args[i].equals("-v")) {
                    debugFlag=true;
                } else if (args[i].equals("-f")) {
                    forceFlag=true;

                } else if (args[i].startsWith("-") && !args[i].startsWith("--") && args[i].length() > 2) {
                    String flags = args[i].substring(1);
                    if (flags.contains("f")) {
                        forceFlag = true;
                    }
                    if (flags.contains("v")) {
                        debugFlag = true;
                    }
                    if (flags.contains("q")) {
                        quietFlag = true;
                    }
                } else if (args[i].startsWith("--")) {
                    continue;
                } else {
                    sb.append("unrecognized argument: \"" + args[i] + "\".\n");
                    accepted = false;
                }
            }
            if (null == nodeName) {
                sb.append("-n nodename is required.\n");
                accepted = false;
            }
            if (null == nodeHostname && null != nodeName) {
                nodeHostname = nodeName;
            }
            if (null == nodeHostname) {
                sb.append("-N nodehostname is required.\n");
                accepted = false;
            }
            if (null == serverHostname) {
                sb.append("-s serverhostname is required.\n");
                accepted = false;
            }
            if (null == baseDir && null != System.getProperty("rdeck.base")) {
                baseDir = System.getProperty("rdeck.base");
            }
            if(!accepted) {
                printUsage();
                throw new CheckException(sb.toString());
            }

            return accepted;
        }

        public Properties getProperties() {
            Properties props = new Properties();
            props.put("rdeck.base", baseDir);
            props.put("framework.node.name", nodeName);
            props.put("framework.node.hostname", nodeHostname);
            props.put("framework.server.hostname", serverHostname);
            return props;

        }
        public String getNodeHostname() {
            return nodeHostname;
        }

        public boolean isQuietFlag() {
            return quietFlag;
        }

        public String getServerHostname() {
            return serverHostname;
        }

        public String getBaseDir() {
            return baseDir;
        }
    }

    /**
     * Exception class
     */
    public static class CheckException extends Exception {
        public CheckException(final String message) {
            super(message);
        }
    }

}
