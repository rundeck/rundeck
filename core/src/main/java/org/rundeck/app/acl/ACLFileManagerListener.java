package org.rundeck.app.acl;

public interface ACLFileManagerListener {
    void aclFileDeleted(String path);

    void aclFileUpdated(String path);
}
