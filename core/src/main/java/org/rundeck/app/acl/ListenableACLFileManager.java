package org.rundeck.app.acl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements Listener semantics for a delegate
 */
public class ListenableACLFileManager
        extends DelegateACLFileManager
{
    private final List<ACLFileManagerListener> listeners = Collections.synchronizedList(new ArrayList<>());

    public ListenableACLFileManager(final ACLFileManager delegate) {
        super(delegate);
    }

    @Override
    public void addListener(final ACLFileManagerListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeListener(final ACLFileManagerListener listener) {
        listeners.remove(listener);
    }

    @Override
    public long storePolicyFile(final String fileName, final InputStream input) {
        long val = super.storePolicyFile(fileName, input);
        notifyUpdated(fileName);
        return val;
    }

    @Override
    public boolean deletePolicyFile(final String fileName) {
        boolean val = super.deletePolicyFile(fileName);
        if (val) {
            notifyDeleted(fileName);
        }
        return val;
    }

    @Override
    public long storePolicyFileContents(final String fileName, final String fileText) {
        long val = super.storePolicyFileContents(fileName, fileText);
        notifyUpdated(fileName);
        return val;
    }


    protected void notifyDeleted(final String fileName) {
        listeners.forEach(a -> a.aclFileDeleted(fileName));
    }

    protected void notifyUpdated(final String fileName) {
        listeners.forEach(a -> a.aclFileUpdated(fileName));
    }

}
