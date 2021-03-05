package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import org.rundeck.storage.api.Path;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class KeyStorageContextProvider
        implements AuthStorageContextProvider
{
    public static final String KEYS_PATH_COMPONENT = "keys";
    public static final String PROJECT_PATH_COMPONENT = "project";
    public static final String PATH_RES_KEY = "path";
    public static final String NAME_RES_KEY = "name";


    /**
     * Map containing path and name given a path
     *
     * @param path path
     *
     * @return map
     */
    @Override
    public Map<String, String> authResForPath(Path path) {
        HashMap<String, String> authResource = new HashMap<String, String>();
        authResource.put(PATH_RES_KEY, path.getPath());
        authResource.put(NAME_RES_KEY, path.getName());
        return authResource;
    }

    /**
     * Generate the environment for a path, based on the convention that /project/name/* maps to a project called
     * "name", and anything else is within the application environment.
     *
     * @param path path
     *
     * @return authorization environment: a project environment if the path matches /project/name/*, otherwise the
     *         application environment
     */
    @Override
    public Set<Attribute> environmentForPath(Path path) {
            return AuthorizationUtil.RUNDECK_APP_ENV;
    }

}
