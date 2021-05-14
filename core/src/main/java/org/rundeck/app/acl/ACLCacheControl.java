package org.rundeck.app.acl;

/**
 * Cache cleanup methods
 */
public interface ACLCacheControl {
    /**
     * Clean acl cache for supplied context and path
     */
    void cleanAclCache(AppACLContext context, String path);
}
