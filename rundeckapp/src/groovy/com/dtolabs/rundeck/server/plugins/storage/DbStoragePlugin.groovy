package com.dtolabs.rundeck.server.plugins.storage

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.storage.StoragePlugin
import com.dtolabs.rundeck.server.storage.NamespacedStorage
import org.rundeck.storage.api.Tree
import org.rundeck.storage.impl.DelegateTree

/**
 * DbStoragePlugin uses a namespace and
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-04-03
 */
@Plugin(name = 'db', service = ServiceNameConstants.Storage)
@PluginDescription(title = 'DB Storage', description = 'Uses DB as storage layer.')
class DbStoragePlugin extends DelegateTree<ResourceMeta> implements StoragePlugin{
    @PluginProperty(title = 'Namespace', description = 'Namespace for storage')
    String namespace;

    NamespacedStorage namespacedStorage;

    Tree<ResourceMeta> delegateTree

    public Tree<ResourceMeta> getDelegate() {
        if (null == delegateTree) {
            if (null == namespacedStorage) {
                throw new IllegalArgumentException("namespacedStorage is not set")
            }
            delegateTree = StorageUtil.resolvedTree(namespace?:null,namespacedStorage)
        }
        return delegateTree;
    }
}
