package rundeck.services.nodes

import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.IProjectNodes
import com.dtolabs.rundeck.core.common.ProjectNodeSupport
import com.dtolabs.rundeck.core.resources.ResourceModelSource

/**
 * Created by greg on 1/15/16.
 */
class CachedProjectNodes implements IProjectNodes {
    @Delegate
    ProjectNodeSupport nodeSupport
    ResourceModelSource source
    INodeSet nodes
    boolean doCache
    Date cacheTime


    @Override
    INodeSet getNodeSet() {
        return doCache?nodes:reloadNodeSet()
    }

    INodeSet reloadNodeSet() {
        nodes = source.getNodes()
        nodes
    }
}
