package com.dtolabs.rundeck.core.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A project node source
 */
public interface IProjectNodes {
    /**
     * Returns the set of nodes for the project
     *
     * @return an instance of {@link com.dtolabs.rundeck.core.common.INodeSet}
     */
    INodeSet getNodeSet() ;

    /**
     * @return the set of exceptions produced by the last attempt to invoke all node providers
     */
    ArrayList<Exception> getResourceModelSourceExceptions();

    /**
     * list the configurations of resource model providers.
     * @return a list of maps containing:
     * <ul>
     * <li>type - provider type name</li>
     * <li>props - configuration properties</li>
     * </ul>
     */
    List<Map> listResourceModelConfigurations();
}
