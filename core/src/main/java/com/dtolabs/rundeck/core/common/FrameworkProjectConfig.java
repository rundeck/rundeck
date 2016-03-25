package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.utils.PropertyLookup;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Config interface
 */
public class FrameworkProjectConfig implements IRundeckProjectConfig, IRundeckProjectConfigModifier {
    public static final String PROP_FILENAME = "project.properties";
    public static final String ETC_DIR_NAME = "etc";
    public static final Logger logger = Logger.getLogger(FrameworkProjectConfig.class);
    private String name;
    /**
     * reference to PropertyLookup object providing access to project.properties
     */
    private PropertyLookup lookup;
    /**
     * Direct projec properties
     */
    private PropertyLookup projectLookup;
    private File propertyFile;
    private FilesystemFramework filesystemFramework;

    private long propertiesLastReload = 0L;

    public FrameworkProjectConfig(
            final String name,
            final File propertyFile,
            final FilesystemFramework filesystemFramework
    )
    {
        this.name = name;
        this.propertyFile = propertyFile;
        this.filesystemFramework = filesystemFramework;
        loadProperties();
    }

    /**
     * Create from existing file
     * @param name
     * @param propertyFile
     * @param filesystemFramework
     * @return
     */
    public static FrameworkProjectConfig create(
            final String name,
            final File propertyFile,
            final FilesystemFramework filesystemFramework
    )
    {
        return new FrameworkProjectConfig(name, propertyFile, filesystemFramework);
    }

    /**
     * Create and generate file with the given properties if not null
     * @param name
     * @param propertyFile
     * @param properties
     * @param filesystemFramework
     * @return
     */
    public static FrameworkProjectConfig create(
            final String name,
            final File propertyFile,
            final Properties properties,
            final FilesystemFramework filesystemFramework
    )
    {

        if (!propertyFile.exists() ) {
            generateProjectPropertiesFile(name, propertyFile, false, properties, true);
        }
        return create(name, propertyFile, filesystemFramework);
    }

    /**
     * Create project.properties file based on $RDECK_BASE/etc/project.properties
     *
     * @param overwrite       Overwrite existing properties file
     * @param properties      properties
     * @param addDefaultProps true to add default properties
     */
    static void generateProjectPropertiesFile(
            final String name,
            final File destfile,
            final boolean overwrite,
            final Properties properties,
            boolean addDefaultProps
    )
    {

        generateProjectPropertiesFile(name, destfile, overwrite, properties, false, null, addDefaultProps);
    }

    /**
     * Update the project properties file by setting updating the given properties, and removing
     * any properties that have a prefix in the removePrefixes set
     *
     * @param properties     new properties to put in the file
     * @param removePrefixes prefixes of properties to remove from the file
     */
    public void mergeProjectProperties(final Properties properties, final Set<String> removePrefixes) {
        generateProjectPropertiesFile(getName(), propertyFile, true, properties, true, removePrefixes, false);
        loadProperties();
    }

    /**
     * Set the project properties file contents exactly
     *
     * @param properties new properties to use in the file
     */
    public void setProjectProperties(final Properties properties) {
        generateProjectPropertiesFile(getName(), propertyFile, true, properties, false, null, false);
        loadProperties();
    }

    @Override
    public void generateProjectPropertiesFile(boolean overwrite, Properties properties, boolean addDefault){
        generateProjectPropertiesFile(getName(), propertyFile, overwrite, properties, false, null, addDefault);
        loadProperties();
    }
    /**
     * Create project.properties file based on $RDECK_BASE/etc/project.properties
     *
     * @param overwrite       Overwrite existing properties file
     * @param properties      properties to use
     * @param merge           if true, merge existing properties that are not replaced
     * @param removePrefixes  set of property prefixes to remove from original
     * @param addDefaultProps true to add default properties
     */
    static void generateProjectPropertiesFile(
            final String name,
            final File destfile,
            final boolean overwrite,
            final Properties properties,
            final boolean merge,
            final Set<String> removePrefixes,
            boolean addDefaultProps
    )
    {
        if (destfile.exists() && !overwrite) {
            return;
        }
        final Properties newProps = new Properties();
        newProps.setProperty("project.name", name);

        //TODO: improve default configuration generation
        if (addDefaultProps) {
            if (null == properties || !properties.containsKey("resources.source.1.type")) {
                //add default file source
                newProps.setProperty("resources.source.1.type", "file");
                newProps.setProperty(
                        "resources.source.1.config.file",
                        new File(destfile.getParentFile(), "resources.xml").getAbsolutePath()
                );
                newProps.setProperty("resources.source.1.config.includeServerNode", "true");
                newProps.setProperty("resources.source.1.config.generateFileAutomatically", "true");
            }
            if (null == properties || !properties.containsKey("service.NodeExecutor.default.provider")) {
                newProps.setProperty("service.NodeExecutor.default.provider", "jsch-ssh");
            }
            if (null == properties || !properties.containsKey("service.FileCopier.default.provider")) {
                newProps.setProperty("service.FileCopier.default.provider", "jsch-scp");
            }
            if (null == properties || !properties.containsKey("project.ssh-keypath")) {
                newProps.setProperty("project.ssh-keypath", new File(
                        System.getProperty("user.home"),
                        ".ssh/id_rsa"
                ).getAbsolutePath());
            }
            if (null == properties || !properties.containsKey("project.ssh-authentication")) {
                newProps.setProperty("project.ssh-authentication", "privateKey");
            }
        }
        if (merge) {
            final Properties orig = new Properties();

            if (destfile.exists()) {
                try {
                    final FileInputStream fileInputStream = new FileInputStream(destfile);
                    try {
                        orig.load(fileInputStream);
                    } finally {
                        fileInputStream.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            //add all original properties that are not in the incoming  properties, and are not
            //matched by one of the remove prefixes
            entry:
            for (final Object o : orig.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                //determine if
                final String key = (String) entry.getKey();
                if (null != removePrefixes) {
                    for (final String replacePrefix : removePrefixes) {
                        if (key.startsWith(replacePrefix)) {
                            //skip this key
                            continue entry;
                        }
                    }
                }
                newProps.put(entry.getKey(), entry.getValue());
            }
        }
        //overwrite original with the input properties
        if (null != properties) {
            newProps.putAll(properties);
        }

        try {
            if(!destfile.getParentFile().exists()){
                destfile.getParentFile().mkdirs();
            }
            final FileOutputStream fileOutputStream = new FileOutputStream(destfile);
            try {
                newProps.store(fileOutputStream, "Project " + name + " configuration, generated");
            } finally {
                fileOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.debug("generated project.properties: " + destfile.getAbsolutePath());
    }

    public File getPropertyFile() {
        return propertyFile;
    }

    private synchronized void checkReloadProperties() {
        if (needsPropertiesReload()) {
            loadProperties();
        }
    }

    private boolean needsPropertiesReload() {
        final File fwkProjectPropertyFile = getFrameworkPropertyFile();
        final long fwkPropsLastModified = fwkProjectPropertyFile.lastModified();
        if (propertyFile.exists()) {
            return propertyFile.lastModified() > propertiesLastReload || fwkPropsLastModified > propertiesLastReload;
        } else {
            return fwkPropsLastModified > propertiesLastReload;
        }
    }

    private File getFrameworkPropertyFile() {
        return new File(
                filesystemFramework.getConfigDir(),
                PROP_FILENAME
        );
    }


    /**
     * Create PropertyLookup for a project from the framework basedir
     *
     * @param filesystemFramework the filesystem
     */
    private static PropertyLookup createDirectProjectPropertyLookup(
            FilesystemFramework filesystemFramework,
            String projectName
    )
    {
        PropertyLookup lookup;
        final Properties ownProps = new Properties();
        ownProps.setProperty("project.name", projectName);

        File projectsBaseDir = filesystemFramework.getFrameworkProjectsBaseDir();
        //generic framework properties for a project

        final File propertyFile = getProjectPropertyFile(new File(projectsBaseDir, projectName));
        final Properties projectProps = PropertyLookup.fetchProperties(propertyFile);

        lookup = PropertyLookup.create(projectProps, PropertyLookup.create(ownProps));
        lookup.expand();
        return lookup;
    }

    private synchronized void loadProperties() {
        //generic framework properties for a project
        final File fwkProjectPropertyFile = getFrameworkPropertyFile();

        loadProperties(fwkProjectPropertyFile);
    }

    private synchronized void loadProperties(final File fwkProjectPropertyFile) {
        //generic framework properties for a project
        lookup = createProjectPropertyLookup(
                filesystemFramework,
                getName()
        );
        projectLookup = createDirectProjectPropertyLookup(
                filesystemFramework,
                getName()
        );

        if (propertyFile.exists()) {
            logger.debug("loading existing project.properties: " + propertyFile.getAbsolutePath());
            final long fwkPropsLastModified = fwkProjectPropertyFile.lastModified();
            final long propsLastMod = propertyFile.lastModified();
            propertiesLastReload = propsLastMod > fwkPropsLastModified ? propsLastMod : fwkPropsLastModified;
        } else {
            logger.debug("loading instance-level project.properties: " + propertyFile.getAbsolutePath());
            propertiesLastReload = fwkProjectPropertyFile.lastModified();
        }
    }


    @Override
    public Date getConfigLastModifiedTime() {
        return new Date(getPropertyFile().lastModified());
    }

    @Override
    public Map<String, String> getProperties() {
        return lookup.getPropertiesMap();
    }

    @Override
    public Map<String, String> getProjectProperties() {
        return projectLookup.getPropertiesMap();
    }

    /**
     * @param name property name
     *
     * @return the property value by name
     */
    @Override
    public synchronized String getProperty(final String name) {
        checkReloadProperties();
        return lookup.getProperty(name);
    }


    /**
     * Create PropertyLookup for a project from the framework basedir
     *
     * @param filesystemFramework the filesystem
     */
    private static PropertyLookup createProjectPropertyLookup(
            FilesystemFramework filesystemFramework,
            String projectName
    )
    {
        PropertyLookup lookup;
        final Properties ownProps = new Properties();
        ownProps.setProperty("project.name", projectName);

        File baseDir = filesystemFramework.getBaseDir();
        File projectsBaseDir = filesystemFramework.getFrameworkProjectsBaseDir();
        //generic framework properties for a project
        final File fwkProjectPropertyFile = FilesystemFramework.getPropertyFile(filesystemFramework.getConfigDir());
        final Properties nodeWideDepotProps = PropertyLookup.fetchProperties(fwkProjectPropertyFile);
        nodeWideDepotProps.putAll(ownProps);

        final File propertyFile = getProjectPropertyFile(new File(projectsBaseDir, projectName));

        if (propertyFile.exists()) {
            lookup = PropertyLookup.create(
                    propertyFile,
                    nodeWideDepotProps,
                    FilesystemFramework.createPropertyLookupFromBasedir(baseDir)
            );
        } else {
            lookup = PropertyLookup.create(fwkProjectPropertyFile,
                                           ownProps, FilesystemFramework.createPropertyLookupFromBasedir(baseDir)
            );
        }
        lookup.expand();
        return lookup;
    }


    /**
     * Get the etc dir from the basedir
     */
    private static File getProjectEtcDir(File baseDir) {
        return new File(baseDir, ETC_DIR_NAME);
    }

    /**
     * Get the project property file from the basedir
     */
    private static File getProjectPropertyFile(File baseDir) {
        return new File(getProjectEtcDir(baseDir), PROP_FILENAME);
    }


    @Override
    public synchronized boolean hasProperty(final String key) {
        checkReloadProperties();
        return lookup.hasProperty(key);
    }

    @Override
    public String getName() {
        return name;
    }
}
