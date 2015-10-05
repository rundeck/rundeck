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
     *
     * @param oldval
     * @param newval
     */
    void trackItem(A oldval, A newval) {
        renamedTrackedItems[oldval] = newval
    }

    /**
     *
     * @param oldval
     * @param newval
     * @return
     */
    boolean trackItemReverted(A oldval, A newval) {
        if (renamedTrackedItems[newval] == oldval) {
            //reverted name change
            renamedTrackedItems.remove(newval)
            return false
        } else {
            trackItem(oldval, newval)
            return true
        }
    }

    @Override
    public String toString() {
        return "RenameTracker{" +
                "renamedTrackedItems=" + renamedTrackedItems +
                '}';
    }
}
