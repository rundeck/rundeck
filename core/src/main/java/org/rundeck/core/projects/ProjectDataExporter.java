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

import com.dtolabs.rundeck.core.plugins.configuration.Property;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface ProjectDataExporter {
    String getName();

    void export(
            String project,
            Object zipBuilder,
            Map<String, String> exportOptions
    );

    /**
     * @return authorization action names, to test, any match will be allowed
     */
    default Collection<String> getExportAuthRequiredActions() {
        return null;
    }

    /**
     * @return true if this type of data can be deselected for export, false if the data should always be exported
     */
    default boolean isExportOptional() {
        return true;
    }

    /**
     * @return true if this type of data should be exported by default for the project, if it is also optional
     */
    default boolean isExportDefault() {
        return true;
    }

    /**
     * @return list of input properties for export process
     */
    default List<Property> getExportProperties() {
        return null;
    }

    default String getExportTitle() {
        return null;
    }

    default String getExportTitleCode() {
        return null;
    }
}
