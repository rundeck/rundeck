package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.utils.IPropertyLookup;
import com.dtolabs.rundeck.core.utils.PropertyLookup;

import java.io.File;

/**
 * Created by greg on 2/20/15.
 */
public class Framework extends FrameworkBase implements IFilesystemFramework {

    private final FilesystemFramework filesystemFramework;
    private IFrameworkProjectMgr filesystemFrameworkProjectManager;

    /**
     * Standard constructor
     */
    Framework(
            final FilesystemFramework filesystemFramework,
            final IFrameworkProjectMgr frameworkProjectMgr,
            final IPropertyLookup lookup,
            final IFrameworkServices services,
            final IFrameworkNodes iFrameworkNodes
    )
    {
        super(frameworkProjectMgr, lookup, services, iFrameworkNodes);
        this.filesystemFramework=filesystemFramework;
        filesystemFramework.setFramework(this);
        this.setFilesystemFrameworkProjectManager(frameworkProjectMgr);
    }
    /**
     * Standard constructor
     */
    Framework(
            final FilesystemFramework filesystemFramework,
            final ProjectManager frameworkProjectMgr,
            final IPropertyLookup lookup,
            final IFrameworkServices services,
            final IFrameworkNodes iFrameworkNodes
    )
    {
        super(frameworkProjectMgr, lookup, services, iFrameworkNodes);
        this.filesystemFramework=filesystemFramework;
        filesystemFramework.setFramework(this);
    }

    public FilesystemFramework getFilesystemFramework() {
        return filesystemFramework;
    }

    @Override
    public File getConfigDir() {
        return getFilesystemFramework().getConfigDir();
    }

    @Override
    public File getFrameworkProjectsBaseDir() {
        return getFilesystemFramework().getFrameworkProjectsBaseDir();
    }

    @Override
    public File getLibextDir() {
        return getFilesystemFramework().getLibextDir(this);
    }

    @Override
    public File getLibextCacheDir() {
        return getFilesystemFramework().getLibextCacheDir(this);
    }

    @Override
    public File getBaseDir() {
        return getFilesystemFramework().getBaseDir();
    }

    public IFrameworkProjectMgr getFilesystemFrameworkProjectManager() {
        return filesystemFrameworkProjectManager;
    }

    public void setFilesystemFrameworkProjectManager(final IFrameworkProjectMgr filesystemFrameworkProjectManager) {
        this.filesystemFrameworkProjectManager = filesystemFrameworkProjectManager;
    }

    @Override
    public ProjectManager getFrameworkProjectMgr() {
        if(null!=getFilesystemFrameworkProjectManager()) {
            return getFilesystemFrameworkProjectManager();
        }
        return super.getFrameworkProjectMgr();
    }


}
