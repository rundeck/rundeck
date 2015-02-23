package com.dtolabs.rundeck.app.internal.framework

import com.dtolabs.rundeck.core.common.FilesystemFramework
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkFactory
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.utils.IPropertyLookup

/**
 * Created by greg on 2/20/15.
 */
class RundeckFrameworkFactory {
    FilesystemFramework frameworkFilesystem
    String type
    ProjectManager dbProjectManager
    ProjectManager filesystemProjectManager
    IPropertyLookup propertyLookup
    Framework createFramework(){
        if(type=='filesystem') {
            return FrameworkFactory.createFramework(propertyLookup, frameworkFilesystem, filesystemProjectManager)
        }else if(type=='db') {
            return FrameworkFactory.createFramework(propertyLookup, frameworkFilesystem, dbProjectManager)
        }else {
            throw new IllegalArgumentException("unsupported type: "+type)
        }
    }
}
