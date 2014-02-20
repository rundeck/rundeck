package com.dtolabs.rundeck.server.plugins.resourcetree

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.resourcetree.ResourceMeta
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.resourcetree.ResourceStoragePlugin
import us.vario.greg.lct.data.file.FileTree
import us.vario.greg.lct.data.file.FileTreeUtil
import us.vario.greg.lct.model.Path
import us.vario.greg.lct.model.Resource
import us.vario.greg.lct.model.Tree

/**
 * FileResourceStoragePlugin is ...
 * @author greg
 * @since 2014-02-19
 */
@Plugin(name = "file", service = ServiceNameConstants.ResourceStorage)
class FileResourceStoragePlugin implements ResourceStoragePlugin {
    @PluginProperty(description = "Base directory", required = true)
    String baseDir


    @Delegate
    Tree<ResourceMeta> delegate

    public Tree<ResourceMeta> getDelegate() {
        if (null == delegate) {
            if(null==baseDir) {
                throw new IllegalArgumentException("baseDir is not set")
            }
            def file = new File(baseDir)
            delegate = FileTreeUtil.forRoot(file,)
        }
        return delegate;
    }
}
