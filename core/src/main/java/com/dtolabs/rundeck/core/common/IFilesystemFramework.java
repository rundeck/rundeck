package com.dtolabs.rundeck.core.common;

import java.io.File;

/**
 * Created by greg on 2/19/15.
 */
public interface IFilesystemFramework {

    /**
     * @return the config dir
     */
    File getConfigDir();

    File getFrameworkProjectsBaseDir();

    /**
     * @return the directory containing plugins/extensions for the framework.
     */
    File getLibextDir();

    /**
     * @return the cache directory used by the plugin system
     */
    File getLibextCacheDir();

    File getBaseDir();
}
