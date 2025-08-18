package org.rundeck.app.auth;

import java.util.List;

public interface RequiredRoleProvider {
    List<String> getRequiredRoles();
}
