package org.rundeck.app.acl;

public interface AppACLContext {
    /**
     * @return true if system level ACL
     */
    boolean isSystem();

    /**
     * @return project name, if project level ACL
     */
    String getProject();

    static AppACLContext system() {
        return new AppACLContextImpl(true, null);
    }

    static AppACLContext project(String project) {
        return new AppACLContextImpl(false, project);
    }
}
