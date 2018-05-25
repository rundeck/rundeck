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

package org.rundeck.plugin.scm.git

/**
 * Created by greg on 10/2/15.
 */
class RenameTracker<A> {
    /**
     * old path -> new path, tracks renamed jobs
     */
    Map<A, A> renamedTrackedItems = Collections.synchronizedMap([:])

    /**
     * @param oldpath
     * @return renamed path if present or null
     */
    A renamedValue(A oldpath) {
        renamedTrackedItems[oldpath]
    }
    /**
     * @param newval
     * @return original value for renamed item, or null
     */
    A originalValue(A newval){
        if(renamedTrackedItems.values().contains(newval)){
            return renamedTrackedItems.keySet().find{renamedTrackedItems[it] == newval}
        }
        null
    }

    boolean wasRenamed(A oldval) {
        renamedValue(oldval) != null
    }

    /**
     * Item was renamed, will remove the mapping if it was a revert
     * @param oldval old
     * @param newval new
     */
    void trackItem(A oldval, A newval) {
        if (oldval == newval) {
            //ignore
            renamedTrackedItems.remove(oldval)
            return
        }
        if (renamedTrackedItems[newval] == oldval) {
            //reverted name change
            renamedTrackedItems.remove(newval)
        } else {
            renamedTrackedItems[oldval] = newval
        }
    }



    @Override
    public String toString() {
        return "RenameTracker{" +
                "renamedTrackedItems=" + renamedTrackedItems +
                '}';
    }
}
