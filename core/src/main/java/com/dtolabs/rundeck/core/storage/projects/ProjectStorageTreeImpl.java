package com.dtolabs.rundeck.core.storage.projects;

import com.dtolabs.rundeck.core.storage.ResourceMeta;
import com.dtolabs.rundeck.core.storage.StorageTreeImpl;
import lombok.Getter;
import org.rundeck.storage.api.Tree;

public class ProjectStorageTreeImpl
        extends StorageTreeImpl
        implements ProjectStorageTree
{

    @Getter private final String project;

    public ProjectStorageTreeImpl(
            final Tree<ResourceMeta> delegate,
            final String project
    )
    {
        super(delegate);
        this.project = project;
    }
}
