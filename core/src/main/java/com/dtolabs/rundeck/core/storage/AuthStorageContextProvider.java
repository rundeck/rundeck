package com.dtolabs.rundeck.core.storage;

import com.dtolabs.rundeck.core.authorization.Attribute;
import org.rundeck.storage.api.Path;

import java.util.Map;
import java.util.Set;

public interface AuthStorageContextProvider {
    Map<String, String> authResForPath(Path path);

    /**
     * Generate the environment for a path, based on the convention that /keys/project/name/* maps to a project called
     * "name", and anything else is within the application environment.
     *
     * @param path path
     * @return authorization environment: a project environment if the path matches /keys/project/name/*, otherwise the
     *         application environment
     */
    Set<Attribute> environmentForPath(Path path);
}
