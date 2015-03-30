package com.dtolabs.rundeck.app.internal.framework

import com.dtolabs.rundeck.core.common.FilesystemFramework
import com.dtolabs.rundeck.core.utils.IPropertyLookup
import com.dtolabs.rundeck.core.utils.PropertyLookup

/**
 * Created by greg on 2/20/15.
 */
class FrameworkPropertyLookupFactory {
    String baseDir
    public IPropertyLookup create(){
        File baseDir = new File(baseDir);

        if (!baseDir.exists()) {
            throw new IllegalArgumentException("rdeck_base directory does not exist. " + baseDir);
        }

        //framework lookup property file
        File propertyFile = FilesystemFramework.getPropertyFile(FilesystemFramework.getConfigDir(baseDir));

        PropertyLookup lookup1 = PropertyLookup.createDeferred(propertyFile);
        lookup1.expand();
        return lookup1;
    }
}
