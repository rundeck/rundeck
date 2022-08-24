package org.rundeck.app.acl;

import java.io.IOException;
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
    private final Listeners listeners = new ListenersManager();

    public ListenableACLFileManager(final ACLFileManager delegate) {
        super(delegate);
    }

    @Override
    public void addListener(final ACLFileManagerListener listener) {
        listeners.addListener(listener);
    }

    @Override
    public void removeListener(final ACLFileManagerListener listener) {
        listeners.removeListener(listener);
    }

    @Override
    public long storePolicyFile(final String fileName, final InputStream input) throws IOException {
        long val = super.storePolicyFile(fileName, input);
        notifyUpdated(fileName);
        return val;
    }

    @Override
    public boolean deletePolicyFile(final String fileName) throws IOException {
        boolean val = super.deletePolicyFile(fileName);
        if (val) {
            notifyDeleted(fileName);
        }
        return val;
    }

    @Override
    public long storePolicyFileContents(final String fileName, final String fileText) throws IOException {
        long val = super.storePolicyFileContents(fileName, fileText);
        notifyUpdated(fileName);
        return val;
    }


    protected void notifyDeleted(final String fileName) {
        listeners.notifyDeleted(fileName);
    }

    protected void notifyUpdated(final String fileName) {
        listeners.notifyUpdated(fileName);
    }

}
