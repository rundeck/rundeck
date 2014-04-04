package com.dtolabs.rundeck.server.storage;

import com.dtolabs.rundeck.core.storage.ExtTree;
import com.dtolabs.rundeck.core.storage.ResourceMeta;

/**
 * NamespacedStorage extends Tree with a String parameter for a namespace.
 *
 * @author Greg Schueler <greg@simplifyops.com>
 * @since 2014-04-03
 */
public interface NamespacedStorage extends ExtTree<String, ResourceMeta> {

}
