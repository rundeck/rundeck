package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.Constants;
import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.core.utils.PropertyLookup;

import java.io.File;

/**
 * Filesystem based configuration for framework
 */
public class FilesystemFramework implements IFilesystemFramework {
    public static final String FRAMEWORK_LIBEXT_DIR = "framework.libext.dir";
    public static final String FRAMEWORK_LIBEXT_CACHE_DIR = "framework.libext.cache.dir";
    public static final String DEFAULT_LIBEXT_DIR_NAME = "libext";
    public static final String DEFAULT_LIBEXT_CACHE_DIR_NAME = "cache";
    public static final String SYSTEM_PROP_LIBEXT = "rdeck.libext";
    public static final String SYSTEM_PROP_LIBEXT_CACHE = "rdeck.libext.cache";
    private File baseDir;
    private File projectsBase;
    private Framework framework;

    public FilesystemFramework(final File baseDir, final File projectsDir) {
        this.baseDir = baseDir;
        this.projectsBase = projectsDir;
    }

    /**
     * @return the path for the projects directory from the basedir
     * @param baseDir base dir
     */
    public static String getProjectsBaseDir(File baseDir) {
        return baseDir + Constants.FILE_SEP + "projects";
    }
    /**
     * @return the config dir
     */
    @Override
    public File getConfigDir() {
        return new File(Constants.getFrameworkConfigDir(getBaseDir().getAbsolutePath()));
    }


    /**
     * @return the config dir for the framework given a basedir
     * @param baseDir base dir
     */
    public static File getConfigDir(File baseDir) {
        return new File(Constants.getFrameworkConfigDir(baseDir.getAbsolutePath()));
    }
    /**
     * Returns an instance of Framework object.  Loads the framework.projects.dir property value, or defaults to basedir/projects
     *
     * @param rdeck_base_dir     path name to the rdeck_base
     * @return a Framework instance
     */
    public static FilesystemFramework getInstanceWithoutProjectsDir(final String rdeck_base_dir) {
        File baseDir = new File(rdeck_base_dir);
        File propertyFile = getPropertyFile(getConfigDir(baseDir));
        String projectsDir=null;
        if(propertyFile.exists()){
            PropertyRetriever propertyRetriever = FilesystemFramework.createPropertyRetriever(baseDir);
            projectsDir = propertyRetriever.getProperty("framework.projects.dir");
        }
        return new FilesystemFramework(new File(rdeck_base_dir), new File(projectsDir));
    }


    @Override
    public File getFrameworkProjectsBaseDir() {
        return projectsBase;
    }

    /**
     * @return the directory containing plugins/extensions for the framework.
     */
    public File getLibextDir(IFramework fwk){
        if(null!=System.getProperty(SYSTEM_PROP_LIBEXT)) {
            return new File(System.getProperty(SYSTEM_PROP_LIBEXT));
        }else if (fwk.getPropertyLookup().hasProperty(FRAMEWORK_LIBEXT_DIR)) {
            return new File(fwk.getPropertyLookup().getProperty(FRAMEWORK_LIBEXT_DIR));
        }else {
            return new File(getBaseDir(), DEFAULT_LIBEXT_DIR_NAME);
        }
    }
    /**
     * @return the directory containing plugins/extensions for the framework.
     */
    @Override
    public File getLibextDir(){
        if(null!=System.getProperty(SYSTEM_PROP_LIBEXT)) {
            return new File(System.getProperty(SYSTEM_PROP_LIBEXT));
        }else {
            return new File(getBaseDir(), DEFAULT_LIBEXT_DIR_NAME);
        }
    }
    /**
     * @return the cache directory used by the plugin system
     */
    @Override
    public File getLibextCacheDir(){
        if (null != System.getProperty(SYSTEM_PROP_LIBEXT_CACHE)) {
            return new File(System.getProperty(SYSTEM_PROP_LIBEXT_CACHE));
        }else {
            return new File(getLibextDir(), DEFAULT_LIBEXT_CACHE_DIR_NAME);
        }

    }
    public File getLibextCacheDir(IFramework fwk){
        if (null != System.getProperty(SYSTEM_PROP_LIBEXT_CACHE)) {
            return new File(System.getProperty(SYSTEM_PROP_LIBEXT_CACHE));
        }else if (fwk.getPropertyLookup().hasProperty(FRAMEWORK_LIBEXT_CACHE_DIR)) {
            return new File(fwk.getPropertyLookup().getProperty(FRAMEWORK_LIBEXT_CACHE_DIR));
        }else {
            return new File(getLibextDir(fwk), DEFAULT_LIBEXT_CACHE_DIR_NAME);
        }
    }


    /**
     * @return the framework property file from the config dir
     * @param configDir config dir
     */
    public static File getPropertyFile(File configDir) {
        return new File(configDir, "framework.properties");
    }

    /**
     * @return Create a safe framework property retriever given a basedir
     * @param baseDir base dir
     */
    public static PropertyRetriever createPropertyRetriever(File baseDir) {
        return createPropertyLookupFromBasedir(baseDir).expand().safe();
    }
    /**
     * @return Create a safe framework property retriever given a basedir
     * @param baseDir base dir
     */
    public static PropertyLookup createPropertyLookupFromBasedir(File baseDir) {
        return PropertyLookup.create(getPropertyFile(getConfigDir(baseDir)));
    }
    /**
     * @return a framework property lookup for this basedir
     */
    @Override
    public IPropertyLookup getPropertyLookup() {
        return createPropertyLookupFromBasedir(baseDir);
    }
    @Override
    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(final File baseDir) {
        this.baseDir = baseDir;
    }

    public Framework getFramework() {
        return framework;
    }

    public void setFramework(final Framework framework) {
        this.framework = framework;
    }
}
