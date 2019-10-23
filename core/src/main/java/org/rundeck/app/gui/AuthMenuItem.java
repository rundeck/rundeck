/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rundeck.app.gui;

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;

import java.util.function.Function;

/**
 * Extends MenuItem by passing AuthContext for any necessary auth checks for enabled checks for the menu item
 */
public interface AuthMenuItem
        extends MenuItem
{

    /**
     * @param auth auth context
     * @return true if enabled, false if disabled
     */
    default boolean isEnabled(UserAndRolesAuthContext auth) {
        return false;
    }

    /**
     * @param auth    auth context
     * @param project name for project oriented items
     * @return true if enabled, false if disabled
     */
    default boolean isEnabled(UserAndRolesAuthContext auth, String project) {
        return false;
    }

    /**
     * Check if the menu item is enabled
     *
     * @param auth        auth context
     * @param project     name for project oriented items
     * @param executionId execution Id for Execution menu items
     * @return true if enabled, false if disabled
     */
    default boolean isEnabledExecution(UserAndRolesAuthContext auth, String project, String executionId) {
        return false;
    }


    /**
     * @param menuType    menu types to check
     * @param authContext available auth context, if AuthMenuItems should be checked
     * @param project     project name, if available and project type should be checked
     * @param executionId execution ID string, if available and execution type should be checked
     * @return enabled check function given the input values
     */
    static Function<MenuItem, Boolean> getEnabledCheck(
            MenuItem.MenuType menuType,
            UserAndRolesAuthContext authContext,
            String project,
            String executionId
    )
    {
        //item is enabled with auth check
        Function<AuthMenuItem, Boolean> typeEnabledWithAuth = (item) ->
                item.getType().isProjectType() && menuType.isProjectType() ? item.isEnabled(authContext, project) :
                item.getType().isExecutionType() && menuType.isExecutionType() ? item.isEnabledExecution(
                        authContext,
                        project,
                        executionId
                ) :
                item.isEnabled(authContext);

        return (item) -> authContext != null && item instanceof AuthMenuItem
                         ? typeEnabledWithAuth.apply((AuthMenuItem) item)
                         : menuType.getEnabledCheck(project, executionId).apply(item);
    }
}
