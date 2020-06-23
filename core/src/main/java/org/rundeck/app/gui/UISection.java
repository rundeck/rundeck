/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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
 * Identifies a "section" of a UI
 */
public interface UISection {
    /**
     * @return the section identifier
     */
    String getSection();

    /**
     * @return the section title if it is a custom section name
     */
    String getSectionTitle();

    static UISection uiLocation(String section, String sectionTitle) {
        return new UISection() {
            @Override
            public String getSection() {
                return section;
            }

            @Override
            public String getSectionTitle() {
                return sectionTitle;
            }
        };
    }
}
