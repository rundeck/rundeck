package com.dtolabs.rundeck.plugins.scm;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.jobs.JobService;
import com.dtolabs.rundeck.core.storage.StorageTree;

/**
 * Context for scm import/export actions, including authorization info and storage tree access
 */
public interface ScmOperationContext {

    /**
     * Get the framework project name
     *
     * @return project name
     */
    public String getFrameworkProject();

    /**
     * ID of a job
     *
     * @return Job ID, or null
     */
    public String getJobId();

    /**
     * @return the authorization context
     */
    public UserAndRolesAuthContext getAuthContext();

    /**
     * @return the storage service
     */
    public StorageTree getStorageTree();

    /**
     * @return username
     */
    public ScmUserInfo getUserInfo();
}
