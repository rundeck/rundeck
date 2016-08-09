/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package com.dtolabs.rundeck.plugins.scm;

/**
 * an item tracked by import plugin, such as a path in a repository.
 */
public interface ScmImportTrackedItem {

    /**
     * @return unique identifier, e.g. relative filepath
     */
    String getId();

    /**
     * @return text title
     */
    String getTitle();

    /**
     * @return e.g. glyphicon-file
     */
    String getIconName();

    /**
     * @return associated Job ID if known
     */
    String getJobId();


    /**
     * @return true if the item is selected
     */
    boolean isSelected();
}
