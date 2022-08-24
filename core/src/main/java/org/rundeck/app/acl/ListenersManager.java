package org.rundeck.app.acl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * can handle a list of listeners and notify of events
 */
public class ListenersManager
        implements Listeners
{

    private final List<ACLFileManagerListener> listeners = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void addListener(final ACLFileManagerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(final ACLFileManagerListener listener) {
        listeners.remove(listener);
    }


    @Override
    public void notifyDeleted(final String fileName) {
        listeners.forEach(a -> a.aclFileDeleted(fileName));
    }

    @Override
    public void notifyUpdated(final String fileName) {
        listeners.forEach(a -> a.aclFileUpdated(fileName));
    }

}
