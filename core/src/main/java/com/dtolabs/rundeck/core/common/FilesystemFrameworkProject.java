package com.dtolabs.rundeck.core.common;

import java.io.File;

/**
 * Created by greg on 2/19/15.
 */
public class FilesystemFrameworkProject {
    private File baseDir;

    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(final File baseDir) {
        this.baseDir = baseDir;
    }
}
