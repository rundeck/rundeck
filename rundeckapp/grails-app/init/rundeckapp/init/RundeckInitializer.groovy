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
package rundeckapp.init

import com.dtolabs.rundeck.core.utils.ZipUtil
import grails.util.Environment
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import rundeckapp.Application
import rundeckapp.cli.CommandLineSetup

import java.nio.file.Path
import java.security.ProtectionDomain
import java.security.SecureRandom
import java.util.jar.JarFile
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.ZipFile

class RundeckInitializer {
    private static final String TEMPLATE_SUFFIX = ".template"
    private static final String LOCATION_SUFFIX = ".location"

    private static final String SERVER_DATASTORE_PATH = "server.datastore.path";
    private static final String CONFIG_DEFAULTS_PROPERTIES = "config-defaults.properties";
    public static final String PROP_LOGINMODULE_NAME = "loginmodule.name";
    public static final String PROP_REALM_LOCATION = "realm.properties.location"

    public static final String RUNDECK_SSL_CONFIG = "rundeck.ssl.config";
    public static final String RUNDECK_KEYSTORE = "keystore";
    public static final String RUNDECK_KEYSTORE_PASSWORD = "keystore.password";
    public static final String RUNDECK_KEY_PASSWORD = "key.password";
    public static final String RUNDECK_TRUSTSTORE = "truststore";
    public static final String RUNDECK_TRUSTSTORE_PASSWORD = "truststore.password";
    public static final String SPRING_BOOT_KEYSTORE_PROP = "server.ssl.key-store"
    public static final String SPRING_BOOT_KEYSTORE_PWD_PROP = "server.ssl.key-store-password"
    public static final String SPRING_BOOT_KEY_PWD_PROP = "server.ssl.key-password"
    public static final String SPRING_BOOT_TRUSTSTORE_PROP = "server.ssl.trust-store"
    public static final String SPRING_BOOT_TRUSTSTORE_PWD_PROP = "server.ssl.trust-store-password"
    public static final String SPRING_BOOT_ENABLE_SSL_PROP = "server.ssl.enabled"
    private static final String LINESEP = System.getProperty("line.separator");

    private File basedir;
    private File serverdir;
    String coreJarName
    File thisJar
    File coreJar
    File configdir;
    File datadir;
    File workdir;
    File toolsdir;
    File toolslibdir;

    /**
     * Config properties are defaulted in config-defaults.properties, but can be overridden by system properties
     */
    final static String[] configProperties = [
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
        "rundeck.config.name",
        "default.encryption.algorithm",
        "default.encryption.password"
    ]

    RundeckInitConfig config

    RundeckInitializer(RundeckInitConfig config) {
        this.config = config
    }

    void initialize() {
        thisJar = thisJarFile();
        initConfigurations()
        setSystemProperties()
        initSsl()

        File installCompleteMarker = new File(config.baseDir+"/var/.install_complete")
        if(!(config.isSkipInstall() || installCompleteMarker.exists())) {
            //installation tasks
            createDirectories()

            coreJarName = "rundeck-core-" + config.appVersion + ".jar";
            coreJar = extractAndLoadCoreJar()
            final File bindir = new File(toolsdir,"bin");
            DEBUG("Extracting bin scripts to: " + config.cliOptions.binDir + " ... ");
            extractBin(bindir, coreJar);
            copyToolLibs(toolslibdir, coreJar);
            if(thisJar.isDirectory()) {
                File sourceTemplateDir = Environment.isDevelopmentEnvironmentAvailable() ?
                                         new File(System.getProperty("user.dir"),"templates") :
                                         new File(thisJar.parentFile.parentFile,"templates")

                expandTemplatesNonJarMode(sourceTemplateDir, config.runtimeConfiguration,serverdir, config.cliOptions.rewrite)
            } else {
                expandTemplates(config.runtimeConfiguration, serverdir, config.cliOptions.rewrite);
            }
            setScriptFilesExecutable(new File(serverdir, "sbin"));
            installCompleteMarker.createNewFile() //mark install as complete so we don't always install
        } else {
            DEBUG("--" + CommandLineSetup.FLAG_SKIPINSTALL + ": Not extracting.");
        }

        if(config.isInstallOnly()) {
            DEBUG("Done. --"+CommandLineSetup.FLAG_INSTALLONLY+": Not starting server.");
            System.exit(0)
        }
    }


    File extractAndLoadCoreJar() {
        coreJar = new File(serverdir,"lib/"+coreJarName)
        if(coreJar.exists()) return coreJar
        if(!coreJar.parentFile.exists()) coreJar.parentFile.mkdirs()
        coreJar.createNewFile()
        if(thisJar.isDirectory()) {
            def coreJarLoc = ((URLClassLoader)Thread.currentThread().contextClassLoader).getURLs().find { it.toString().endsWith(coreJarName) }
            coreJarLoc.withInputStream {
                coreJar << it
            }
        } else {
            ZipFile springBootJar = new ZipFile(thisJar)
            coreJar << springBootJar.getInputStream(springBootJar.getEntry("WEB-INF/lib/"+coreJarName))
        }
        coreJar
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
        final String depslist;
        final String[] jars;

        try {
            JarFile zf = new JarFile(coreJar)
            depslist = zf.getManifest().getMainAttributes().getValue("Rundeck-Tools-Dependencies");
        } catch(Exception ex) { }
        if (null == depslist) {
            throw new RuntimeException(
                    "Rundeck Core jar file manifest attribute \"Rundeck-Tools-Dependencies\" was not found: " + coreJar
                            .getAbsolutePath());
        }
        jars = depslist.split(" ");

        //copy jars from list to toolslibdir
        if(this.thisJar.isDirectory()) {
            copyLibsToToolsNonJar(toolslibdir, jars)
        } else {
            final String jarpath = "WEB-INF/lib";
            for (final String jarName : jars) {
                ZipUtil.extractZip(thisJar.getAbsolutePath(), toolslibdir, jarpath + "/" + jarName, jarpath + "/");
                if (!new File(toolslibdir, jarName).exists()) {
                    ERR(
                            "Failed to extract dependent jar for tools into " + toolslibdir.getAbsolutePath() +
                            ": " +
                            jarName
                    );
                }
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

    void copyLibsToToolsNonJar(final File toolslibdir, final String[] jarNamesToCopy) {
        def classlibList = ((URLClassLoader)Thread.currentThread().contextClassLoader).getURLs()
        jarNamesToCopy.each { jarName ->
            def sourceJar = classlibList.find { it.toString().endsWith(jarName) }
            if(sourceJar) {
                File jarDest = new File(toolslibdir,jarName)
                jarDest << sourceJar.newInputStream()
            } else {
                ERR("Failed to extract dependant jar ${jarName} into tools dir: ${toolslibdir.absolutePath}")
            }
        }
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
        for (final String sname : destDir.list(new FilenameFilter() {
            public boolean accept(final File file, final String s) {
                return !s.endsWith(".bat");
            }
        })) {
            final File script = new File(destDir, sname);
            if(!script.setExecutable(true)) {
                ERR("Unable to set executable permissions for file: " + script.getAbsolutePath());
            }
        }
    }

    /**
     * Return file for the enclosing jar
     *
     * @return
     *
     * @throws URISyntaxException
     */
    private static File thisJarFile() {
        ProtectionDomain protectionDomain
        if(Environment.isDevelopmentEnvironmentAvailable()) {
            protectionDomain = Application.class.getProtectionDomain();
        } else {
            Class baseClass = tryToLoadCorrectBaseClass()
            protectionDomain = baseClass.getProtectionDomain()
        }

        final URL location = protectionDomain.getCodeSource().getLocation();

        try {
            return new File(location.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    static Class tryToLoadCorrectBaseClass() {
        //For spring boot launched jar this will be correct
        try {
            return ClassLoader.getSystemClassLoader().loadClass("org.springframework.boot.loader.Launcher")
        } catch(Exception ex) {}
        //Otherwise use the regular class loader and use any class in it
        return Application.class
    }

    /**
     * Look for *.template files in the directory, duplicate to file "name" and expand properties
     *
     * @param props
     * @param directory
     * @param overwrite
     */
    void expandTemplatesNonJarMode(final File sourceTemplateDir, final Properties props, final File destinationDirectory, final boolean overwrite) throws
            IOException
    {
        if (!destinationDirectory.isDirectory() && !destinationDirectory.mkdirs()) {
            throw new RuntimeException("Unable to create config dir: " + destinationDirectory.getAbsolutePath());
        }

        Path sourceDirPath = sourceTemplateDir.toPath()
        sourceTemplateDir.traverse(type: groovy.io.FileType.FILES, nameFilter: ~/.*\.template/) { templateFile ->
            copyToDestinationAndExpandProperties(destinationDirectory,sourceDirPath,templateFile,props,overwrite)
        }
    }

    void copyToDestinationAndExpandProperties(File destDir, Path sourceDirPath, File sourceTemplate, Properties props, boolean overwrite) {
        String renamedDestFileName = sourceTemplate.name.substring(0, sourceTemplate.name.length() - TEMPLATE_SUFFIX.length())
        String destinationFilePath = props.getProperty(renamedDestFileName+LOCATION_SUFFIX) ?: destDir.absolutePath +"/" + sourceDirPath.relativize(sourceTemplate.parentFile.toPath()).toString()+"/"+renamedDestFileName

        File destinationFile = new File(destinationFilePath)

        if(!overwrite && destinationFile.exists()) return
        if(!destinationFile.parentFile.exists()) destinationFile.parentFile.mkdirs()
        DEBUG("Writing config file: " + destinationFile.getAbsolutePath());
        expandTemplate(sourceTemplate.newInputStream(),destinationFile.newOutputStream(),props)
    }

    void setSystemProperties() {
        System.setProperty("server.http.port", config.runtimeConfiguration.getProperty("server.http.port"));
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR, forwardSlashPath(config.baseDir));
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR, forwardSlashPath(config.configDir));
        System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_DATA_DIR, forwardSlashPath(config.serverBaseDir));
        if(config.cliOptions.projectDir) {
            System.setProperty(RundeckInitConfig.SYS_PROP_RUNDECK_PROJECTS_DIR, config.cliOptions.projectDir);
        }

        if(!System.getProperty(RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION)) {
            System.setProperty(
                    RundeckInitConfig.SYS_PROP_RUNDECK_CONFIG_LOCATION, new File(
                    config.configDir, config.runtimeConfiguration.getProperty(
                    RundeckInitConfig.RUNDECK_CONFIG_NAME_PROP
            )
            ).getAbsolutePath()
            );
        }

        if (config.useJaas) {
            System.setProperty("java.security.auth.login.config", new File(config.configDir,
                                                                           config.runtimeConfiguration.getProperty("loginmodule.conf.name")).getAbsolutePath());
            System.setProperty(PROP_LOGINMODULE_NAME, config.runtimeConfiguration.getProperty(PROP_LOGINMODULE_NAME));
        }
    }

    private void initConfigurations() {
        final Properties defaults = loadDefaults(CONFIG_DEFAULTS_PROPERTIES);
        final Properties configuration = createConfiguration(defaults);
        configuration.put(PROP_REALM_LOCATION, forwardSlashPath(config.configDir)
                + "/realm.properties");
        config.runtimeConfiguration = configuration
    }

    void createDirectories() {
        basedir = new File(config.baseDir)
        if(!basedir.exists()) basedir.mkdirs()
        serverdir = createDir(config.serverBaseDir, basedir, "server")
        configdir = createDir(config.configDir, serverdir, "config")
        datadir = createDir(config.dataDir, serverdir, "data")
        workdir = createDir(config.workDir, serverdir, "work")
        toolsdir = createDir(null,basedir,"tools")
        toolslibdir = createDir(null,toolsdir,"lib")
        createDir(null,basedir,"var")
    }

    File createDir(String specifiedPath, File base, String child) {
        File dir
        if(specifiedPath) {
            dir = new File(specifiedPath)
        } else {
            dir = new File(base, child)
        }
        dir.mkdirs()
        return dir
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
            Resource configDefaultsResource = new ClassPathResource(CONFIG_DEFAULTS_PROPERTIES)
            properties.load(configDefaultsResource.inputStream)
        } catch (IOException e) {
            e.printStackTrace()
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
        properties.put(RundeckInitConfig.SYS_PROP_RUNDECK_BASE_DIR, config.baseDir);
        properties.put(RundeckInitConfig.SERVER_DATASTORE_PATH, forwardSlashPath(config.dataDir) + "/grailsdb");
        properties.put(RundeckInitConfig.LOG_DIR, forwardSlashPath(config.serverBaseDir) + "/logs");
        properties.put(RundeckInitConfig.SYS_PROP_RUNDECK_SERVER_CONFIG_DIR, forwardSlashPath(config.configDir));
        properties.put(RundeckInitConfig.LAUNCHER_JAR_LOCATION, forwardSlashPath(thisJar.getAbsolutePath()));
        properties.put("default.encryption.password", randomString(15));
        for (final String configProperty : configProperties) {
            if (null != System.getProperty(configProperty)) {
                properties.put(configProperty, forwardSlashPath(System.getProperty(configProperty)));
            }
        }
        DEBUG(properties.toString())
        return properties;
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
     * Generate random password for encrypt
     *
     * @param length
     */
    public static String randomString(final int length) {
        SecureRandom random = new SecureRandom();
        return String.format(
            "%" + length + "s", new BigInteger(length * 5, random).toString(32)
        ).replace('\u0020', '0');
    }


    /**
     * this method sets ssl system properties so that spring boot can pick them up and configure the server properly
     */
    private void initSsl() {
        if(null!=System.getProperty(RUNDECK_SSL_CONFIG)){
            final Properties sslProperties = new Properties();
            try{
                sslProperties.load(new FileInputStream(System.getProperty(RUNDECK_SSL_CONFIG)));
            } catch (IOException e) {
                System.err.println("Could not load specified rundeck.ssl.config file: " + System.getProperty(
                        RUNDECK_SSL_CONFIG) + ": " + e.getMessage());
                e.printStackTrace(System.err);
            }
            System.setProperty(SPRING_BOOT_KEYSTORE_PROP,sslProperties.getProperty(RUNDECK_KEYSTORE))
            System.setProperty(SPRING_BOOT_KEYSTORE_PWD_PROP,sslProperties.getProperty(RUNDECK_KEYSTORE_PASSWORD))
            System.setProperty(SPRING_BOOT_KEY_PWD_PROP,sslProperties.getProperty(RUNDECK_KEY_PASSWORD))
            System.setProperty(SPRING_BOOT_TRUSTSTORE_PROP,sslProperties.getProperty(RUNDECK_TRUSTSTORE))
            System.setProperty(SPRING_BOOT_TRUSTSTORE_PWD_PROP,sslProperties.getProperty(RUNDECK_TRUSTSTORE_PASSWORD))
            System.setProperty("server.port", config.runtimeConfiguration.getProperty("server.https.port"))
            System.setProperty(SPRING_BOOT_ENABLE_SSL_PROP,"true")
        }
    }

    static String forwardSlashPath(final String input) {
        if (System.getProperties().get("file.separator").equals("\\")) {
            return input.replaceAll("\\\\", "/");
        }
        return input;
    }

    /**
     * Look for *.template files in the directory, duplicate to file "name" and expand properties
     *
     * @param props
     * @param directory
     * @param overwrite
     */
    void expandTemplates(final Properties props, final File directory, final boolean overwrite) throws
            IOException
    {
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
        final def renamer = new ZipUtil.renamer() {
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
        List<File> origNames = new ArrayList<>();
        List<File> partNames = new ArrayList<>();
        /**
         * accept .template files in templates/ directory
         * and only accept if destination file doesn't exist, or overwrite==true
         */
        final FilenameFilter filenameFilter = new FilenameFilter() {
            public boolean accept(final File file, final String name) {
                if(!(name.startsWith(tmplPrefix) && name.endsWith(tmplSuffix))) return;
                final String destName = renamer.rename(name);
                final File destFile;
                if (null != props.getProperty(destName + ".location")) {
                    destFile = new File(props.getProperty(destName + ".location"));
                } else {
                    destFile = new File(file, destName);
                }

                final boolean accept = (overwrite || !destFile.isFile());
                if (accept) {
                    DEBUG("Writing config file: " + destFile.getAbsolutePath());
                    if (!destFile.getName().contains("._")) {
                        origNames.add(destFile);
                    } else {
                        partNames.add(destFile);
                    }
                }
                return accept;
            }
        };

        ZipUtil.extractZip(thisJar.getAbsolutePath(), directory,
                           filenameFilter,
                           renamer,
                           //expand properties in-place
                           new propertyExpander(props)
        );
        Set<File> parts = processFileParts(origNames);
        partNames.removeAll(parts);
        for (File part : parts) {
            //unprocessed
            part.delete();
        }
    }

    private Set<File> processFileParts(final List<File> origNames) throws IOException {
        //process appending file parts
        Set<File> parts = new HashSet<>();
        for (File origName : origNames) {
            int i = 1;
            File test = new File(origName.getParentFile(), origName.getName() + "._" + i);
            while (test.exists()) {
                //append to original
                FileUtils.appendFile(test, origName);
                test.delete();
                parts.add(test);
                i++;
                test = new File(origName.getParentFile(), origName.getName() + "._" + i);
            }
        }
        return parts;
    }



    private static class propertyExpander implements ZipUtil.streamCopier {
        Properties properties;

        public propertyExpander(final Properties properties) {
            this.properties = properties;
        }

        public void copyStream(final InputStream inputStream, final OutputStream outputStream) throws IOException {
            expandTemplate(inputStream, outputStream, properties);
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

    private static final String PROPERTY_PATTERN = '\\$\\{([^\\}]+?)\\}';

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
        if (config.cliOptions.debug) {
            System.err.println("VERBOSE: " + msg);
        }
    }

}
