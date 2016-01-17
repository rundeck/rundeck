package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.Policies;

import java.io.File;

/**
 * Created by greg on 7/27/15.
 */
public class AuthorizationFactory {
    /**
     * Create authorization from files in a directory
     *
     * @param dir dir path
     *
     * @return authorization
     */
    public static Authorization createFromDirectory(File dir) {
        return AclsUtil.createAuthorization(Policies.load(dir));
    }
}
