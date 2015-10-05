package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import com.dtolabs.rundeck.core.storage.StorageTree;

/**
 * Builds {@link ScmOperationContext}
 */
public class ScmOperationContextBuilder {
    private ScmOperationContextImpl impl;

    private ScmOperationContextBuilder() {
        impl = new ScmOperationContextImpl();
    }

    /**
     * @return new blank builder
     */
    public static ScmOperationContextBuilder builder() {
        return new ScmOperationContextBuilder();
    }

    /**
     * @param context source context
     *
     * @return new builder based on the source context
     */
    public static ScmOperationContextBuilder builder(ScmOperationContext context) {
        ScmOperationContextBuilder builder = new ScmOperationContextBuilder();
        builder.impl = new ScmOperationContextImpl(context);
        return builder;
    }

    /**
     * set the project name
     *
     * @param frameworkProject project name
     *
     * @return this builder
     */
    public ScmOperationContextBuilder frameworkProject(final String frameworkProject) {
        impl.frameworkProject = frameworkProject;
        return this;
    }

    /**
     * Set the job ID
     *
     * @param jobId job id
     *
     * @return this
     */
    public ScmOperationContextBuilder jobId(final String jobId) {
        impl.jobId = jobId;
        return this;
    }

    /**
     * Set the auth context
     *
     * @param authContext auth context
     *
     * @return this
     */
    public ScmOperationContextBuilder authContext(final UserAndRolesAuthContext authContext) {
        impl.authContext = authContext;
        return this;
    }

    /**
     * Set the storage tree
     *
     * @param storageTree storage tree
     *
     * @return this
     */
    public ScmOperationContextBuilder storageTree(final StorageTree storageTree) {
        impl.storageTree = storageTree;
        return this;
    }

    /**
     * Set the user info
     *
     * @param userInfo user info
     *
     * @return this
     */
    public ScmOperationContextBuilder userInfo(final ScmUserInfo userInfo) {
        impl.userInfo = userInfo;
        return this;
    }

    /**
     * @return built context
     */
    public ScmOperationContext build() {
        return new ScmOperationContextImpl(impl);
    }

    /**
     * Created by greg on 9/29/15.
     */
    static class ScmOperationContextImpl implements ScmOperationContext {
        private String frameworkProject;
        private UserAndRolesAuthContext authContext;
        private StorageTree storageTree;
        private ScmUserInfo userInfo;
        private String jobId;

        public ScmOperationContextImpl() {
        }

        public ScmOperationContextImpl(ScmOperationContext context) {
            this.frameworkProject = context.getFrameworkProject();
            this.authContext = context.getAuthContext();
            this.storageTree = context.getStorageTree();
            this.userInfo = context.getUserInfo();
            this.jobId = context.getJobId();
        }

        @Override
        public String getFrameworkProject() {
            return frameworkProject;
        }

        @Override
        public UserAndRolesAuthContext getAuthContext() {
            return authContext;
        }

        @Override
        public StorageTree getStorageTree() {
            return storageTree;
        }

        @Override
        public ScmUserInfo getUserInfo() {
            return userInfo;
        }

        @Override
        public String getJobId() {
            return jobId;
        }
    }
}