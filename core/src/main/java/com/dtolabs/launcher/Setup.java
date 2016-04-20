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

package com.dtolabs.launcher;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.cli.CLIToolLogger;
import org.apache.log4j.Logger;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.FilterSetCollection;
import org.apache.tools.ant.util.FileUtils;

import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;

/**
 * Setup which replaces old command line parsing at the shell layer which leaves the old setup/setup.bat to only
 * blindly pass arguments along with the java.home, rdeck.base.
 */
public class Setup implements CLIToolLogger {
    public static final Logger logger = Logger.getLogger(Setup.class);
    /**
     * basic bootstrapped rdeck.base
     */
    public static String RDECK_BASE = Constants.getSystemBaseDir();
    /**
     * setup usage statement
     */
    public static final String SETUP_USAGE = "rd-setup [-v] -n nodename [-N hostname] [ --key=value ]";

    /**
      * force a rewrite of the framework configuration files. always true
      */
    public static final boolean FORCE_FLAG = true;
    private Parameters parameters = new Parameters();
    public static final String TEMPLATE_RESOURCES_PATH = "com/dtolabs/launcher/setup/templates";

    /**
     * default constructor
     */
    public Setup() {
        //empty
    }

    /**
     * called from setup shell/bat script. Calls the {@link #execute} method.
     * @param args args
     */
    public static void main(final String args[]) {
        int exitCode = 1;
        final Setup setup = new Setup();
        try {
            setup.execute(args);
            exitCode = 0;
        } catch (Throwable exc) {
            System.err.println("ERROR: " + exc.getMessage());
        }
        System.exit(exitCode);
    }

    /**
     * Template files to copy to RDECK_BASE/etc dir (without .template extension)
     */
    static String[] templates={
        "admin.aclpolicy",
        "apitoken.aclpolicy",
        "framework.properties",
        "cli-log4j.properties",
        "profile.bat",
        "profile",
        "project.properties"
    };
    static final HashSet<String> restrictedPermTemplates=new HashSet<String>();
    static {
        restrictedPermTemplates.addAll(Arrays.asList(
            "admin.aclpolicy","apitoken.aclpolicy"));
    }
    /**
     * Validates the install, generates preference data and then invokes the adminCmd.xml
     *
     * @param args Command line args
     * @throws SetupException thrown if error
     */
    public void execute(final String[] args) throws SetupException {
        parameters.parse(args);
        performSetup(args);
    }

    public void performSetup() throws SetupException {
        performSetup(new String[0]);
    }
    private void performSetup(String[] args) throws SetupException {
        parameters.validate();
        validateInstall();
        final File basedir = new File(parameters.getBaseDir());
        if(!basedir.exists()){
            if(!basedir.mkdirs()) {
                throw new SetupException("Unable to create RDECK_BASE directory: " + parameters.getBaseDir());
            }
        }

        generatePreferences(args, parameters.getProperties());
        newImpl(basedir);
    }

    private void newImpl(File basedir) throws SetupException {
        // create dirs
        File etcdir = new File(Constants.getFrameworkConfigDir(basedir.getAbsolutePath()));
        if(!etcdir.exists() && !etcdir.mkdir()){
            throw new SetupException("Unable to create directory: " + etcdir.getAbsolutePath());
        }
        final boolean overwrite =
            parameters.forceFlag ;

        //2. generate all RDECK_BASE/etc properties files if overwrite

        final File preferences = new File(etcdir, "preferences.properties");
        final Properties prefs = new Properties();
        if(preferences.isFile()){
            final FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(preferences);
                try {
                    prefs.load(fileInputStream);
                } finally {
                    fileInputStream.close();
                }
            } catch (IOException e) {
                throw new SetupException("Error loading file: " + preferences.getAbsolutePath(), e);
            }
        }
        long time = System.currentTimeMillis();
        try {
            for (final String filename : templates) {
                final File destFile = new File(etcdir, filename);
                final File templFile = getTemplateFile(filename+".template");
                if (overwrite  && destFile.isFile()) {
                    //create backup
                    final File backup = new File(etcdir, filename + ".backup-" + time);
                    FileUtils.getFileUtils().copyFile(destFile, backup);
                }
                if (overwrite || !destFile.isFile()) {
                    //copy template to destination, using filtering
                    FilterSetCollection filterset = new FilterSetCollection();
                    final FilterSet set = new FilterSet();
                    set.setFiltersfile(preferences);

                    filterset.addFilterSet(set);
                    FileUtils.getFileUtils().copyFile(templFile,destFile,filterset,true);
                    if(restrictedPermTemplates.contains(filename)) {
                        //remove write permissions of target file
                        if (!destFile.setWritable(false, false)) {
                            logger.warn("Failed to remove writable flag for file: " + destFile.getAbsolutePath());
                        }
                    }

                }
            }
        } catch (IOException e) {
            throw new SetupException("Error copying templates", e);
        }
        //////////
        //4. mkdir necessary dirs
        //load framework.properties
        final Properties frameworkProps = new Properties();
        try {
            final FileInputStream fileInputStream = new FileInputStream(new File(etcdir, "framework.properties"));
            try {
                frameworkProps.load(fileInputStream);
            } finally {
                fileInputStream.close();
            }
        } catch (IOException e) {
            throw new SetupException("unable to load framework.properties", e);
        }
        for (final String prop : new String[]{
            "framework.var.dir",
            "framework.tmp.dir",
            "framework.logs.dir",
            "framework.etc.dir",
            "framework.projects.dir"}) {
            final String path = frameworkProps.getProperty(prop);
            final String expandpath = path.replaceAll("\\$\\{framework\\.var\\.dir\\}", frameworkProps.getProperty(
                "framework.var.dir"));

            final File dir = new File(expandpath);
            if (!dir.isDirectory() && !dir.mkdirs()) {
                throw new SetupException("Unable to create dir: " + dir.getAbsolutePath());
            }
        }
    }


    /**
     * Look for template in the jar resources, otherwise look for it on filepath
     * @param filename template name
     * @return file
     * @throws java.io.IOException on io error
     */
    private File getTemplateFile(String filename) throws IOException {
        File templateFile=null;
        final String resource = TEMPLATE_RESOURCES_PATH + "/" + filename;
        InputStream is = Setup.class.getClassLoader().getResourceAsStream(resource);
        if (null == is) {
            throw new RuntimeException("Unable to load required template: " + resource);
        }
        templateFile = File.createTempFile("temp", filename);
        try {
            return copyToNativeLineEndings(is, templateFile);
        } finally {
            is.close();
        }
    }

    private File copyToNativeLineEndings(InputStream input, File destFile) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(input));
        final OutputStream out= new FileOutputStream(destFile);
        try{
            final BufferedWriter writer=new BufferedWriter(new OutputStreamWriter(out));
            String line=bufferedReader.readLine();
            while (line != null) {
                writer.write(line);
                writer.newLine();
                line = bufferedReader.readLine();
            }
            writer.flush();
        }finally {
            out.close();
        }
        return destFile;

    }

    /**
     * Checks the basic install assumptions
     */
    private void validateInstall() throws SetupException {
        // check if rdeck.base is defined.
        if (null == parameters.getBaseDir() || parameters.getBaseDir().equals("")) {
            throw new SetupException("rdeck.base property not defined or is the empty string");
        }
        if (!checkIfDir("rdeck.base", parameters.getBaseDir())) {
            throw new SetupException(parameters.getBaseDir() + " is not a valid rdeck install");
        }
    }

    // check if path exists as a directory
    private boolean checkIfDir(final String propName, final String path) {
        if (null == path || path.equals("")) {
            throw new IllegalArgumentException(propName + "property had null or empty value");
        }
        return new File(path).exists();
    }


    // generate the preferences.properties file
    private void generatePreferences(final String args[], Properties input) throws SetupException {
        File frameworkPreferences = new File(Constants.getFrameworkPreferences(parameters.getBaseDir()));

        try {
            Preferences.generate(args, frameworkPreferences.getAbsolutePath(), input);
        } catch (Exception e) {
            throw new SetupException("failed generating setup preferences: " + e.getMessage(), e);
        }
        // check the preferences.properties file
        if (!frameworkPreferences.exists()) {
            throw new SetupException("Unable to generate preferences file: " + frameworkPreferences);
        }
        if (!frameworkPreferences.isFile()) {
            throw new SetupException(frameworkPreferences + " preferences file is not a regular file");
        }
    }


    public static void printUsage() {
        System.out.println(SETUP_USAGE);

    }

    private static void usageError(final String msg) {
        System.err.println("\nERROR: " + msg);
        printUsage();
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void error(String message) {
        System.err.println(message);
    }

    public void warn(String message) {
//        if (null != requiredParams && requiredParams.debugFlag) {
            System.err.println(message);
//        }
    }

    public void verbose(String message) {
//        if(null!=requiredParams && requiredParams.debugFlag){
            System.out.println(message);
//        }
    }
    public void debug(String message) {
//        if(null!=requiredParams && requiredParams.debugFlag){
            System.out.println(message);
//        }
    }

    public Parameters getParameters() {
        return parameters;
    }


    /* Inner class representing required parameters
     * also sets corresponding java system properties
     */
    public static class Parameters {

        private boolean forceFlag = Setup.FORCE_FLAG;
        private boolean debugFlag;
        private String serverHostname;
        private String serverName;
        private String baseDir;
        private Properties properties;

        public Parameters() {
            properties = new Properties();
        }

        protected boolean getForceFlag() {
            return forceFlag;
        }

        protected boolean getDebugFlag() {
            return debugFlag;
        }

        protected String getNodeArg() {
            return serverName;
        }

        /**
         * get parameter for an option
         *
         * @param args  command line arg vector
         * @param arg_i arg index
         * @return arg value
         * @throws SetupException thrown if param missing a required arg
         */
        private String getOptParam(final String args[], final int arg_i) throws SetupException {
            if (arg_i == (args.length - 1))
                throw new SetupException("option: " + args[arg_i] + " must take a parameter");

            if (args[arg_i + 1].startsWith("-"))
                throw new SetupException("arg: " + args[arg_i] + " does not have a parameter, instead was provided: " + args[arg_i + 1]);

            return args[arg_i + 1];
        }

        /**
         * process the required single hyphen parameters.
         * support basic opt args: debugFlag (-v), and name (-n <name>)
         *
         * @param args command line arg vector
         * @throws SetupException thrown if missing required arg
         */
        private void parse(final String args[]) throws SetupException {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-v")) {
                    debugFlag = true;
                } else if (args[i].equals("-n")) {
                    serverName = getOptParam(args, i);
                    i++;
                } else if (args[i].equals("-N")) {
                    serverHostname = getOptParam(args, i);
                    i++;
                } else if (args[i].equals("-d")) {
                    baseDir = getOptParam(args, i);
                    i++;
                } else if (args[i].startsWith("--")) {
                    continue;
                } else {
                    usageError("unrecognized argument: \"" + args[i] + "\"");
                }
            }
        }
        public void validate() throws SetupException {
            if (null == serverName) {
                throw new SetupException("server name not specified.");
            }
            if (null == serverHostname) {
                serverHostname = serverName;
            }
            if (null == baseDir) {
                baseDir = Constants.getSystemBaseDir();
            }

            properties.setProperty("rdeck.base", Preferences.forwardSlashPath(baseDir));
            properties.setProperty("framework.server.hostname", serverHostname);
            properties.setProperty("framework.server.name", serverName);
        }

        public String getNodeHostnameArg() {
            return serverHostname;
        }

        public String getServerHostname() {
            return serverHostname;
        }

        public Properties getProperties() {
            return properties;
        }


        public void setServerHostname(String serverHostname) {
            this.serverHostname = serverHostname;
        }

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public String getBaseDir() {
            return baseDir;
        }

        public void setBaseDir(String baseDir) {
            this.baseDir = baseDir;
        }

        public void setProperty(String name, String value) {
            properties.setProperty(name, value);
        }
    }

    /**
     * Exception class
     */
    public static class SetupException extends Exception {
        public SetupException() {
        }

        public SetupException(String s) {
            super(s);
        }

        public SetupException(String s, Throwable throwable) {
            super(s, throwable);
        }

        public SetupException(Throwable throwable) {
            super(throwable);
        }
    }

}
