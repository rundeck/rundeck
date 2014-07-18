/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/*
 * Note that any non java/javax from the JavaSE runtime that are included in this file must also
 * be copied into the resulting rundeck-lanucher.jar via rundeckapp/scripts/BuildLauncher.groovy.
 */

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * ExpandRunServer extracts some contents to particular locations, generates config files based on internal templates,
 * and then loads and starts the jetty server configured for the application.
 */
public class ExpandRunServer {

    
    
    private static final String CONFIG_DEFAULTS_PROPERTIES = "config-defaults.properties";
    //system props for launcher config
    private static final String SYS_PROP_RUNDECK_LAUNCHER_DEBUG = "rundeck.launcher.debug";
    private static final String SYS_PROP_RUNDECK_LAUNCHER_REWRITE = "rundeck.launcher.rewrite";
    private static final String SYS_PROP_RUNDECK_JAASLOGIN = "rundeck.jaaslogin";
    private static final String RUN_SERVER_CLASS = "com.dtolabs.rundeck.RunServer";

    private static final String PROP_LOGINMODULE_NAME = "loginmodule.name";
    private static final String SERVER_DATASTORE_PATH = "server.datastore.path";
    private static final String RUNDECK_SERVER_CONFIG_DIR = "rundeck.server.configDir";
    
    /**
     * Config properties are defaulted in config-defaults.properties, but can be overridden by system properties
     */
    final static String[] configProperties = {
        "server.http.port",
        "server.https.port",
        "server.hostname",
        "server.web.context",
        "rdeck.base",
        SERVER_DATASTORE_PATH,
        "default.user.name",
        "default.user.password",
        PROP_LOGINMODULE_NAME,
        "loginmodule.conf.name",
        "rundeck.config.name"
    };
    /**
     * line separator
     */
    private static final String LINESEP = System.getProperty("line.separator");
    public static final String FLAG_INSTALLONLY = "installonly";
    public static final String FLAG_SKIPINSTALL = "skipinstall";

    //members
    String basedir;

    File serverdir;
    File configDir;
    File datadir;
    File thisJar;
    String coreJarName;
    boolean debug = false;
    boolean rewrite = false;
    boolean useJaas = true;
    String versionString;
    private String runClassName;
    private String jettyLibsString;
    private String jettyLibPath;
    private static final String RUNDECK_START_CLASS = "Rundeck-Start-Class";
    private static final String RUNDECK_JETTY_LIBS = "Rundeck-Jetty-Libs";
    private static final String RUNDECK_JETTY_LIB_PATH = "Rundeck-Jetty-Lib-Path";
    private static final String RUNDECK_VERSION = "Rundeck-Version";
    
    private final Options options = new Options();

    public static void main(final String[] args) throws Exception {
        new ExpandRunServer().run(args);
    }

    @SuppressWarnings("static-access")
    public ExpandRunServer() {
        
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
        
        debug = Boolean.getBoolean(SYS_PROP_RUNDECK_LAUNCHER_DEBUG);
        rewrite = Boolean.getBoolean(SYS_PROP_RUNDECK_LAUNCHER_REWRITE);
        useJaas = null == System.getProperty(SYS_PROP_RUNDECK_JAASLOGIN) || Boolean.getBoolean(
            SYS_PROP_RUNDECK_JAASLOGIN);
        runClassName = RUN_SERVER_CLASS;
        thisJar = thisJarFile();
        //load jar attributes
        final Attributes mainAttributes = getJarMainAttributes();
        if (null == mainAttributes) {
            throw new RuntimeException("Unable to load attributes");
        }

        versionString = mainAttributes.getValue(RUNDECK_VERSION);
        if (null != versionString) {
            DEBUG("Rundeck version: " + versionString);
        } else {
            throw new RuntimeException("Jar file attribute not found: " + RUNDECK_VERSION);
        }
        runClassName = mainAttributes.getValue(RUNDECK_START_CLASS);
        if (null == runClassName) {
            throw new RuntimeException("Jar file attribute not found: " + RUNDECK_START_CLASS);
        }
        jettyLibsString = mainAttributes.getValue(RUNDECK_JETTY_LIBS);
        if (null == jettyLibsString) {
            throw new RuntimeException("Jar file attribute not found: " + RUNDECK_JETTY_LIBS);
        }
        jettyLibPath = mainAttributes.getValue(RUNDECK_JETTY_LIB_PATH);
        if (null == jettyLibPath) {
            throw new RuntimeException("Jar file attribute not found: " + RUNDECK_JETTY_LIB_PATH);
        }
    }
    
    public void run(final String[] args) throws Exception {
        
        final CommandLineParser parser = new GnuParser();
        
        final CommandLine cl;
        try {
            cl = parser.parse(this.options, args);
            
            if(cl.hasOption('h')) {
                printUsage();
                return;
            }
            if(cl.hasOption(FLAG_INSTALLONLY) && cl.hasOption(FLAG_SKIPINSTALL)) {
                ERR("--" + FLAG_INSTALLONLY + " and --" + FLAG_SKIPINSTALL + " are mutually exclusive");
                printUsage();
                System.exit(1);
                return;
            }
            
        } catch (ParseException e) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + e.getMessage() );
            System.exit(1);
            return;
        }
        debug = debug || cl.hasOption('d');
        DEBUG("Debugging is turned on.");
        //nb: absolutePath called twice because a relative file will return null for getParentFile
        this.basedir = cl.getOptionValue('b', new File(thisJar.getAbsolutePath()).getParentFile().getAbsolutePath());
        this.serverdir = new File(cl.getOptionValue("serverdir", basedir+"/server"));
        this.configDir = new File(cl.getOptionValue("c", serverdir + "/config"));
        this.datadir = new File(cl.getOptionValue("datadir", serverdir + "/data"));
        DEBUG("configDir is " + configDir.getAbsolutePath());
        final File toolsdir = new File(basedir, "tools");
        final File toolslibdir = new File(toolsdir, "lib");
        final File bindir = new File(cl.getOptionValue('x', toolsdir.getAbsolutePath() + "/bin"));


        initArgs();
        this.coreJarName = "rundeck-core-" + versionString + ".jar";

        if (null != basedir) {
            System.setProperty("rdeck.base", forwardSlashPath(basedir));
        }

        final Properties defaults = loadDefaults(CONFIG_DEFAULTS_PROPERTIES);
        final Properties configuration = createConfiguration(defaults);
        configuration.put("realm.properties.location", forwardSlashPath(configDir.getAbsolutePath())
                                                       + "/realm.properties");
        DEBUG("Runtime configuration properties: " + configuration);
        
        if(!cl.hasOption(FLAG_SKIPINSTALL)) {
            final File libdir = new File(serverdir, "lib");
            DEBUG("Extracting libs to: " + libdir.getAbsolutePath() + " ... ");
            deleteExistingJarsInDir(libdir, "^rundeck.*");
            extractLibs(libdir);
            extractJettyLibs(libdir);
            final File expdir = new File(serverdir, "exp");
            DEBUG("Extracting webapp to: " + expdir.getAbsolutePath() + " ... ");

            deleteExistingJarsInDir(new File(expdir, "webapp/WEB-INF/lib"), "^rundeck.*");
            extractWar(expdir);
            
            DEBUG("Extracting bin scripts to: " + bindir.getAbsolutePath() + " ... ");

            extractBin(bindir, new File(serverdir, "exp/webapp/WEB-INF/lib/" + coreJarName));
            deleteExistingJarsInDir(toolslibdir, "^rundeck.*");
            copyToolLibs(toolslibdir, new File(serverdir, "exp/webapp/WEB-INF/lib/" + coreJarName));
        
            expandTemplates(configuration, serverdir, rewrite);
            setScriptFilesExecutable(new File(serverdir, "sbin"));
//            extractLauncherContents(new File(basedir, "docs"), "docs", "docs/");
//            extractLauncherContents(new File(basedir, "libext"), "libext", "libext/");
        }else{
            DEBUG("--" + FLAG_SKIPINSTALL + ": Not extracting.");
        }
        
        if(cl.hasOption('p')) {
            System.setProperty("rdeck.projects",cl.getOptionValue('p'));
        }

        if (cl.hasOption(FLAG_INSTALLONLY)) {
            DEBUG("Done. --"+FLAG_INSTALLONLY+": Not starting server.");
        } else {
            execute(cl.getArgs(), configDir, new File(basedir), serverdir, configuration);
        }
    }

    private void printUsage() {
        // automatically generate the help statement
        final HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp( "java [JAVA_OPTIONS] -jar rundeck-launcher.jar ", "\nRun the rundeck server, installing the " +
                "necessary components if they do not exist.\n", options,
                "\nhttp://rundeck.org\n", true );
    }

    private void extractLauncherContents(final File targetdir, final String prefix, final String stripPrefix) throws IOException {
        if(!targetdir.exists() && !targetdir.mkdirs()) {
            ERR("Unable to create dir: " + targetdir);
        }
        //extract launcher contents dir into the targetdir
        ZipUtil.extractZip(thisJar.getAbsolutePath(), targetdir, prefix, stripPrefix);
    }

    /**
     * Set executable bit on any script files in the directory if it exists
     * @param sbindir
     */
    private void setScriptFilesExecutable(final File sbindir) {
        //set executable on files
        if (sbindir.exists()) {
            for (final String s : sbindir.list()) {
                final File script = new File(sbindir, s);
                if (script.isFile() && !script.setExecutable(true)) {
                    ERR("Unable to set executable permissions for file: " + script.getAbsolutePath());
                }
            }
        }
    }

    private void copyToolLibs(final File toolslibdir, final File coreJar) throws IOException {
        if (!toolslibdir.isDirectory()) {
            if (!toolslibdir.mkdirs()) {
                ERR("Couldn't create bin dir: " + toolslibdir.getAbsolutePath());
                return;
            }
        }
        //get dependencies info
        final JarFile zf = new JarFile(coreJar);
        final String depslist = zf.getManifest().getMainAttributes().getValue("Rundeck-Tools-Dependencies");
        if (null == depslist) {
            throw new RuntimeException(
                "Rundeck Core jar file manifest attribute \"Rundeck-Tools-Dependencies\" was not found: " + coreJar
                    .getAbsolutePath());
        }
        final String[] jars = depslist.split(" ");

        //copy jars from list to toolslibdir

        final String jarpath = jettyLibPath;
        for (final String jarName : jars) {
            ZipUtil.extractZip(thisJar.getAbsolutePath(), toolslibdir, jarpath + "/" + jarName, jarpath + "/");
            if(!new File(toolslibdir,jarName).exists()) {
                ERR("Failed to extract dependent jar for tools into "+ toolslibdir.getAbsolutePath()+": " + jarName);
            }
        }

        //finally, copy corejar to toolslibdir
        final File destfile = new File(toolslibdir, coreJarName);
        if(!destfile.exists()) {
            if(!destfile.createNewFile()) {
                ERR("Unable to create file: " + destfile.createNewFile());
            }
        }
        ZipUtil.copyStream(new FileInputStream(coreJar), new FileOutputStream(destfile));
    }

    /**
     * Extract scripts to bin dir
     *
     * @param destDir
     */
    private void extractBin(final File destDir, final File coreJar) throws IOException {
        if (!destDir.isDirectory()) {
            if (!destDir.mkdirs()) {
                ERR("Couldn't create bin dir: " + destDir.getAbsolutePath());
                return;
            }
        }
        ZipUtil.extractZip(coreJar.getAbsolutePath(), destDir, "com/dtolabs/rundeck/core/cli/templates",
            "com/dtolabs/rundeck/core/cli/templates/");

        //set executable on shell scripts
        for (final String s : destDir.list(new FilenameFilter() {
            public boolean accept(final File file, final String s) {
                return !s.endsWith(".bat");
            }
        })) {
            final File script = new File(destDir, s);
            if(!script.setExecutable(true)) {
                ERR("Unable to set executable permissions for file: " + script.getAbsolutePath());
            }
        }
    }

    /**
     * Look for *.template files in the directory, duplicate to file "name" and expand properties
     *
     * @param props
     * @param directory
     * @param overwrite
     */
    private void expandTemplates(final Properties props, final File directory, final boolean overwrite) throws
        IOException {
        if (overwrite) {
            DEBUG("Configuration overwrite is TRUE");
        }
        final String tmplPrefix = "templates/";
        final String tmplSuffix = ".template";
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new RuntimeException("Unable to create config dir: " + directory.getAbsolutePath());
        }

        /**
         * rename by removing suffix and prefix dir
         */
        final ZipUtil.renamer renamer = new ZipUtil.renamer() {
            public String rename(String input) {
                if (input.endsWith(tmplSuffix)) {
                    input = input.substring(0, input.length() - tmplSuffix.length());
                }
                if (input.startsWith(tmplPrefix)) {
                    input = input.substring(tmplPrefix.length());
                }
                return input;
            }
        };
        /**
         * accept .template files in templates/ directory
         * and only accept if destination file doesn't exist, or overwrite==true
         */
        final FilenameFilter filenameFilter = new FilenameFilter() {
            public boolean accept(final File file, final String name) {
                final String destName = renamer.rename(name);
                final File destFile;
                if (null != props.getProperty(destName + ".location")) {
                    destFile = new File(props.getProperty(destName + ".location"));
                } else {
                    destFile = new File(file, destName);
                }

                final boolean accept = (overwrite || !destFile.isFile())
                                       && name.startsWith(tmplPrefix)
                                       && name.endsWith(tmplSuffix);
                if (accept) {
                    DEBUG("Writing config file: " + destFile.getAbsolutePath());
                }
                return accept;
            }
        };
        ZipUtil.extractZip(thisJar.getAbsolutePath(), directory,
            filenameFilter,
            renamer,
            //expand properties in-place
            new propertyExpander(props));
    }

    private static class propertyExpander implements ZipUtil.streamCopier {
        Properties properties;

        public propertyExpander(final Properties properties) {
            this.properties = properties;
        }

        public void copyStream(final InputStream in, final OutputStream out) throws IOException {
            expandTemplate(in, out, properties);
        }
    }

    /**
     * Copy from file to toFile, expanding properties in the contents
     *
     * @param inputStream  input stream
     * @param outputStream output stream
     * @param props        properties
     */
    private static void expandTemplate(final InputStream inputStream, final OutputStream outputStream,
                                       final Properties props) throws IOException {

        final BufferedReader read = new BufferedReader(new InputStreamReader(inputStream));
        final BufferedWriter write = new BufferedWriter(new OutputStreamWriter(outputStream));
        String line = read.readLine();
        while (null != line) {
            write.write(expandProperties(props, line));
            write.write(LINESEP);
            line = read.readLine();
        }
        write.flush();
        write.close();
        read.close();
    }

    /**
     * Load properties file with default values in the jar
     *
     * @param path
     *
     * @return
     */
    private Properties loadDefaults(final String path) {
        final Properties properties = new Properties();
        try {
            final InputStream is = loadResourceInternal(CONFIG_DEFAULTS_PROPERTIES);
            if (null == is) {
                throw new RuntimeException("Unable to read config-defaults.properties from jar");
            }
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException("Unable to load config defaults: " + path + ": " + e.getMessage(), e);
        }
        return properties;
    }


    /**
     * Create properties for template expansion
     *
     * @return
     */
    private Properties createConfiguration(final Properties defaults) throws UnknownHostException {
        final Properties properties = new Properties();
        properties.putAll(defaults);
        final String localhostname = getHostname();
        if (null != localhostname) {
            properties.put("server.hostname", localhostname);
        }
        properties.put("rdeck.base", forwardSlashPath(basedir));
        properties.put(SERVER_DATASTORE_PATH, forwardSlashPath(datadir.getAbsolutePath()) + "/grailsdb");
        properties.put("rundeck.log.dir", forwardSlashPath(serverdir.getAbsolutePath()) + "/logs");
        properties.put("rundeck.launcher.jar.location", forwardSlashPath(thisJar.getAbsolutePath()));
        properties.put(RUNDECK_SERVER_CONFIG_DIR, forwardSlashPath(this.configDir.getAbsolutePath()));
        for (final String configProperty : configProperties) {
            if (null != System.getProperty(configProperty)) {
                properties.put(configProperty, forwardSlashPath(System.getProperty(configProperty)));
            }
        }

        return properties;
    }
    public static String forwardSlashPath(final String input) {
        if (System.getProperties().get("file.separator").equals("\\")) {
            return input.replaceAll("\\\\", "/");
        }
        return input;
    }

    private String getHostname() {
        String name = null;
        try {
            name = InetAddress.getLocalHost().getHostName();
            DEBUG("Determined hostname: " + name);
        } catch (UnknownHostException ignored) {
        }
        return name;
    }

    /**
     * Extract any jars in the lib/ resource dir to the destination
     *
     * @param libdir
     *
     * @throws IOException
     */
    private void extractLibs(final File libdir) throws IOException {
        //expand contents
        ZipUtil.extractZip(thisJar.getAbsolutePath(), libdir, "lib/", "lib/");
    }

    /**
     * Use the jar attributes to extract selective libs for jetty dependencies
     *
     * @param libdir
     *
     * @throws IOException
     */
    private void extractJettyLibs(final File libdir) throws IOException {
        //expand contents
        final String[] jarNames = jettyLibsString.split(" ");

        final String jarpath = jettyLibPath;
        for (final String jarName : jarNames) {
            ZipUtil.extractZip(thisJar.getAbsolutePath(), libdir, jarpath + "/" + jarName, jarpath + "/");
        }
    }

    private void extractWar(final File expdir) throws IOException {
        //expand contents
        ZipUtil.extractZip(thisJar.getAbsolutePath(), expdir, "pkgs", "pkgs/");
    }

    /**
     * Remove any jar files whose names match the pattern that exist in the directory, if the directory
     * exists.
     * @param dir directory
     * @param fileMatch regex to match files to delete
     */
    private void deleteExistingJarsInDir(final File dir, final String fileMatch) {
        if(dir.isDirectory()){
            final File[] rundeckJars = dir.listFiles(new FilenameFilter() {
                public boolean accept(final File file, final String s) {
                    return s.matches(fileMatch) && s.endsWith(".jar");
                }
            });
            for (final File rundeckJar : rundeckJars) {
                DEBUG("Delete existing jar file: " + rundeckJar.getAbsolutePath());
                if (!rundeckJar.delete()) {
                    ERR("Unable to remove existing jar file: " + rundeckJar);
                }
            }
        }
    }

    private void execute(final String[] args, final File configDir, final File baseDir, final File serverDir,
                         final Properties configuration) throws
        IOException {
        //set some system properties used by the RunServer class
        System.setProperty("server.http.port", configuration.getProperty("server.http.port"));
        System.setProperty(RUNDECK_SERVER_CONFIG_DIR, configDir.getAbsolutePath());
        System.setProperty("rundeck.server.serverDir", serverDir.getAbsolutePath());
        System.setProperty("rundeck.config.location", new File(configDir, configuration.getProperty(
            "rundeck.config.name")).getAbsolutePath());
        if (useJaas) {
            System.setProperty("java.security.auth.login.config", new File(configDir,
                configuration.getProperty("loginmodule.conf.name")).getAbsolutePath());
            System.setProperty(PROP_LOGINMODULE_NAME, configuration.getProperty(PROP_LOGINMODULE_NAME));
        }

        //configure commandline arguments
        final ArrayList<String> execargs = new ArrayList<String>();
        execargs.add(baseDir.getAbsolutePath());
        if (args.length > 1) {
            execargs.addAll(Arrays.asList(Arrays.copyOfRange(args.clone(), 1, args.length)));
        }

        //execute the RunServer.main method
        int result = 500;
        try {
            invokeMain(runClassName, execargs.toArray(new String[execargs.size()]), new File(
                serverdir, "lib"));
            result = 0;//success
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        DEBUG("Finished, exit code: " + result);
        System.exit(result);
    }

    /**
     * Invoke the main method on the given class, using the specified args, with a classloader including all jars in the
     * specified libdir
     *
     * @param CLASSNAME class to invoke
     * @param args      arguments to pass to main method
     * @param libdir    dir containing required jar files
     *
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     * @throws MalformedURLException
     */
    public void invokeMain(final String CLASSNAME, final String[] args, final File libdir) throws NoSuchMethodException,
        InvocationTargetException,
        IllegalAccessException, ClassNotFoundException, MalformedURLException {
        final ClassLoaderUtil clload = new ClassLoaderUtil(libdir);
        // load the class
        final ClassLoader loader = clload.getClassLoader(ClassLoader.getSystemClassLoader());

        final Class cls = Class.forName(CLASSNAME, true, loader);
        // invokde the main method via reflection
        final Method mainMethod = ClassLoaderUtil.findMain(cls);

        Thread.currentThread().setContextClassLoader(loader);
        DEBUG("Start server with " + CLASSNAME + ".main(" + Arrays.toString(args) + ")");
        mainMethod.invoke(null, new Object[]{args});
    }


    /**
     * Return inputstream for a resource from the enclosing jar file
     *
     * @param path resource path
     *
     * @return InputStream for the resource
     *
     * @throws IOException if an error occurs
     */
    private InputStream loadResourceInternal(final String path) throws IOException {
        final ZipFile jar = new ZipFile(thisJar);
        return jar.getInputStream(new ZipEntry(path));
    }

    /**
     * Initialize field values based on parsed args and system properties.  Sets the basedir to parent dir of the
     * launcher jar if unset, and loads necessary manifest attributes for extracting the launcher jar contents.
     */
    private void initArgs() {
        if (null == basedir) {
            //locate basedir based on this jar's location
            //set basedir to the dir containing
            final File base = new File(thisJar.getAbsolutePath()).getParentFile();
            basedir = base.getAbsolutePath();
            LOG("Rundeck basedir: " + basedir);
        }

    }


    /**
     * Load the manifest main attributes from the enclosing jar
     *
     * @return
     */
    private static Attributes getJarMainAttributes() {
        Attributes mainAttributes = null;
        try {
            final File file = thisJarFile();
            final JarFile jarFile = new JarFile(file);
            mainAttributes = jarFile.getManifest().getMainAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mainAttributes;
    }

    /**
     * Return file for the enclosing jar
     *
     * @return
     *
     * @throws URISyntaxException
     */
    private static File thisJarFile() {
        final ProtectionDomain protectionDomain = ExpandRunServer.class.getProtectionDomain();
        final URL location = protectionDomain.getCodeSource().getLocation();
        try {
            return new File(location.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Print log message
     *
     * @param s
     */
    private void LOG(final String s) {
        System.out.println(s);
    }
    /**
     * Print err message
     *
     * @param s
     */
    private void ERR(final String s) {
        System.err.println("ERROR: " + s);
    }


    /**
     * Print debug message if debug is enabled
     *
     * @param msg
     */
    private void DEBUG(final String msg) {
        if (debug) {
            System.err.println("VERBOSE: " + msg);
        }
    }

    private static final String PROPERTY_PATTERN = "\\$\\{([^\\}]+?)\\}";

    /**
     * Return the input with embedded property references expanded
     *
     * @param properties the properties to select form
     * @param input      the input
     *
     * @return string with references expanded
     */
    public static String expandProperties(final Properties properties, final String input) {
        final Pattern pattern = Pattern.compile(PROPERTY_PATTERN);
        final Matcher matcher = pattern.matcher(input);
        final StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            final String match = matcher.group(1);
            if (null != properties.get(match)) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(properties.getProperty(match)));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
