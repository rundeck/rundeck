package com.dtolabs.rundeck.core.storage.projects;

import com.dtolabs.rundeck.core.storage.StorageTree;
import org.rundeck.app.spi.AppService;

/**
 * Access to the StorageTree associated with a Project, may be a subtree view of the Project's full tree
 */
public interface ProjectStorageTree
        extends StorageTree, AppService
{
    String getProject();

    static ProjectStorageTree withTree(StorageTree tree, String project) {
        return new ProjectStorageTreeImpl(tree, project);
    }
}
