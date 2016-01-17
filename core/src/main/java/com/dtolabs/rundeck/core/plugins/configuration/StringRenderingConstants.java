/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.plugins.configuration;

/**
 * Constants that govern the different ways a {@link Property.Type#String} can be rendered.
 * 
 * @author Kim Ho <a href="mailto:kim.ho@salesforce.com">kim.ho@salesforce.com</a>
 */
public class StringRenderingConstants {
    
    public static final String SELECTION_ACCESSOR_KEY = "selectionAccessor";
    public static final String VALUE_CONVERSION_KEY = "valueConversion";
    public static final String VALUE_CONVERSION_FAILURE_KEY = "valueConversionFailure";
    public static final String VALUE_CONVERSION_FAILURE_REMOVE = "remove";
    public static final String INSTANCE_SCOPE_NODE_ATTRIBUTE_KEY = "instance-scope-node-attribute";
    public static final String STORAGE_PATH_ROOT_KEY = "storage-path-root";
    public static final String STORAGE_FILE_META_FILTER_KEY = "storage-file-meta-filter";
    /**
     * Rendering option key to set the display type of a String property
     */
    public static final String DISPLAY_TYPE_KEY = "displayType";
    public static final String STATIC_TEXT_CONTENT_TYPE_KEY = "staticTextContentType";
    public static final String GROUPING = "grouping";
    public static final String GROUP_NAME = "groupName";

    /**
     * Values that can be specified for a key of {@link #DISPLAY_TYPE_KEY}
     */
    public enum DisplayType {
        SINGLE_LINE,
        MULTI_LINE,
        STATIC_TEXT,
        PASSWORD,
        CODE;
        public boolean equalsOrString(Object o) {
            return this == o || toString().equals(o);
        }
    }
    public enum SelectionAccessor{
        STORAGE_PATH;

        public boolean equalsOrString(Object o) {
            return this == o || toString().equals(o);
        }
    }
    public enum ValueConversion{
        STORAGE_PATH_AUTOMATIC_READ,
        PRIVATE_DATA_CONTEXT;

        public boolean equalsOrString(Object o) {
            return this == o || toString().equals(o);
        }
    }
}
