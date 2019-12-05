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
package org.rundeck.core.projects;

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface ProjectDataImporter {
    /**
     *
     * @return The identifier associated with this importer
     */
    String getSelector();

    /**
     *
     * @param authContext
     * @param project
     * @param importFile
     * @param importOptions
     * @return Errors encountered during the import process that should be reported to the user
     */
    List<String> doImport(UserAndRolesAuthContext authContext, String project, File importFile, Map importOptions);
}
