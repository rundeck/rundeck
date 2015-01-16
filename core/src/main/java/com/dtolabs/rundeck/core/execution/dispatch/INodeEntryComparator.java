/*
 * Copyright 2012 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* INodeEntryComparator.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 1/6/12 9:18 AM
* 
*/
package com.dtolabs.rundeck.core.execution.dispatch;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.INodeSet;

import java.util.*;

/**
 * INodeEntryComparator is ...
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class INodeEntryComparator implements Comparator<INodeEntry> {
    private final String rankProperty;

    public INodeEntryComparator(final String rankProperty) {
        this.rankProperty = rankProperty;
    }

    /**
     * Utility method to return nodes ordered by rank property
     *
     * @param nodes node set
     * @param rankProperty rank property
     * @param rankAscending true if ascending
     *
     * @return ordered node list
     */
    public static List<INodeEntry> rankOrderedNodes(INodeSet nodes, String rankProperty,
            boolean rankAscending) {
        return rankOrderedNodes(nodes.getNodes(), rankProperty, rankAscending);
    }

    /**
     * Utility method to return nodes ordered by rank property
     *
     * @param nodes node collection
     * @param rankProperty rank property
     * @param rankAscending true if ascending
     *
     * @return ordered node list
     */
    public static List<INodeEntry> rankOrderedNodes(Collection<INodeEntry> nodes, String rankProperty,
            boolean rankAscending) {
        final ArrayList<INodeEntry> nodes1 = new ArrayList<INodeEntry>(nodes);
        //reorder based on configured rank property and order
        final INodeEntryComparator comparator = new INodeEntryComparator(rankProperty);

        Collections.sort(nodes1, rankAscending ? comparator : Collections.reverseOrder(comparator));
        return nodes1;
    }

    public int compare(final INodeEntry iNodeEntry, final INodeEntry iNodeEntryB) {
        final String valA, valB;
        if (null != rankProperty) {
            valA = iNodeEntry.getAttributes().get(rankProperty);
            valB = iNodeEntryB.getAttributes().get(rankProperty);
        } else {
            valA = iNodeEntry.getNodename();
            valB = iNodeEntryB.getNodename();
        }
        if (null != valA && null != valB) {
            //try numeric comparison
            Long numA = null, numB = null;
            try {
                numA = Long.parseLong(valA);
            } catch (NumberFormatException e) {
            }
            try {
                numB = Long.parseLong(valB);
            } catch (NumberFormatException e) {
            }
            if (null != numA && null != numB) {
                final int i = numA.compareTo(numB);
                if (0 != i) {
                    return i;
                }
            }
            int i = valA.compareTo(valB);
            if (i != 0) {
                return i;
            }
        } else if (null != valA) {
            return -1;
        } else if (null != valB) {
            return 1;
        }
        return iNodeEntry.getNodename().compareTo(iNodeEntryB.getNodename());
    }
}
