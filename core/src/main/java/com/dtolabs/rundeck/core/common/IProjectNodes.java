/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.WriteableModelSource;

import java.util.*;

/**
 * A project node source
 */
public interface IProjectNodes {
    /**
     * Returns the set of nodes for the project
     *
     * @return an instance of {@link com.dtolabs.rundeck.core.common.INodeSet}
     */
    INodeSet getNodeSet();

    /**
     * @return all sources
     */
    public List<ReadableProjectNodes> getResourceModelSources();
    /**
     * @return writeable sources
     */
    public Collection<WriteableProjectNodes> getWriteableResourceModelSources();

    /**
     * Contains and identifies a model source entry for the project
     */
    static interface ReadableProjectNodes {
        /**
         * @return The source
         */
        ResourceModelSource getSource();

        /**
         * @return config index
         */
        int getIndex();

        /**
         * @return provider type
         */
        String getType();
    }

    /**
     * Contains and identifies a writeable model source entry for the project
     */
    static interface WriteableProjectNodes {
        /**
         * @return the writeable source
         */
        WriteableModelSource getWriteableSource();

        /**
         * @return config index
         */
        int getIndex();

        /**
         * @return provider type
         */
        String getType();
    }
    /**
     * @return the set of exceptions produced by the last attempt to invoke all node providers
     */
    ArrayList<Exception> getResourceModelSourceExceptions();

    /**
     * @return the set of exceptions produced by source name
     */
    Map<String,Exception> getResourceModelSourceExceptionsMap();

    /**
     * list the configurations of resource model providers.
     *
     * @return a list of maps containing:
     * <ul>
     * <li>type - provider type name</li>
     * <li>props - configuration properties</li>
     * </ul>
     */
    List<Map<String, Object>> listResourceModelConfigurations();


}
