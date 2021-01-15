package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.AuthorizationUtil;
import com.dtolabs.rundeck.core.common.Framework;
import com.dtolabs.rundeck.core.common.FrameworkProject;
import org.rundeck.storage.api.Path;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ProjectKeyStorageContextProvider
        implements AuthStorageContextProvider
{
    public static final String KEYS_PATH_COMPONENT = "keys";
    public static final String PROJECT_PATH_COMPONENT = "project";
    public static final String PATH_RES_KEY = "path";
    public static final String NAME_RES_KEY = "name";

    @Override
    public Map<String, String> authResForPath(Path path) {
        HashMap<String, String> authResource = new HashMap<String, String>();
        authResource.put(PATH_RES_KEY, path.getPath());
        authResource.put(NAME_RES_KEY, path.getName());
        return authResource;
    }

    /**
     * Generate the environment for a path, based on the convention that /keys/project/name/* maps to a project called
     * "name", and anything else is within the application environment.
     *
     * @param path path
     * @return authorization environment: a project environment if the path matches /keys/project/name/*, otherwise the
     *         application environment
     */
    @Override
    public Set<Attribute> environmentForPath(Path path) {
        String[] paths = path.getPath().split("/");
        Set<Attribute> env = new HashSet<Attribute>(AuthorizationUtil.RUNDECK_APP_ENV);
        if (matchesProjectKeysPath(paths)) {
            env.addAll(FrameworkProject.authorizationEnvironment(paths[2]));
        }
        return env;
    }

    private boolean matchesProjectKeysPath(final String[] paths) {
        return paths != null && paths.length >= 3 && paths[0].equals(KEYS_PATH_COMPONENT) && paths[1].equals(
                PROJECT_PATH_COMPONENT);
    }

}
