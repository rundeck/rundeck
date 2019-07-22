/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

/**
 * Define a menu item with a link
 */
public interface MenuItem {
    /**
     * Location of menu item
     */
    public MenuType getType();

    enum MenuDomain {
        SYSTEM,
        PROJECT,
        EXECUTION
        //todo
    }
    enum MenuType {
        /**
         *
         */
        PROJECT(MenuDomain.PROJECT),
        PROJECT_CONFIG(MenuDomain.PROJECT),
        SYSTEM_CONFIG(MenuDomain.SYSTEM),
        USER_MENU(MenuDomain.SYSTEM),
        EXECUTION_RETRY(MenuDomain.EXECUTION);

        MenuType(final MenuDomain domain) {
            this.domain = domain;
        }

        private final MenuDomain domain;

        public boolean isProjectType() {
            return domain == MenuDomain.PROJECT;
        }

        public boolean isExecutionType() {
            return domain == MenuDomain.EXECUTION;
        }
    }

    /**
     * i18n message code for title
     */
    String getTitleCode();

    /**
     * Default title string
     */
    String getTitle();

    /**
     * Location HREF
     */
    default String getHref() {
        return null;
    }

    /**
     * @param project
     */
    default String getProjectHref(String project) {
        return null;
    }
    default String getExecutionHref(String project, String executionId) {
        return null;
    }

    /**
     * @return css class string for icon in certain menu locations, or null for a default, e.g. 'fas fa-check' for
     *         font-awesome, or 'glyphicon glyphicon-ok' for glyphicon
     */
    default String getIconCSS() {
        return null;
    }

    /**
     * @return true if enabled, false if disabled
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * @param project name for project oriented items
     * @return true if enabled, false if disabled
     */
    default boolean isEnabled(String project) {
        return true;
    }

    /**
     * @param project name for project oriented items
     * @param executionId execution Id for Execution menu items
     * @return true if enabled, false if disabled
     */
    default boolean isEnabledExecution(String project, String executionId) {
        return true;
    }

}
