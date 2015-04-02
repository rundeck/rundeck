package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.core.utils.PropertyLookup;

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

    /**
     * @return a framework property lookup for this basedir
     */
    IPropertyLookup getPropertyLookup();

    File getBaseDir();
}
