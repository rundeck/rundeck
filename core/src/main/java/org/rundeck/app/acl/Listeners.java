package org.rundeck.app.acl;

public interface Listeners {
    void addListener(ACLFileManagerListener listener);

    void removeListener(ACLFileManagerListener listener);

    void notifyDeleted(String fileName);

    void notifyUpdated(String fileName);
}
