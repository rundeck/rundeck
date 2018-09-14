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

    enum MenuType {
        /**
         *
         */
        PROJECT,
        PROJECT_CONFIG,
        SYSTEM_CONFIG,
        USER_MENU
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

}
