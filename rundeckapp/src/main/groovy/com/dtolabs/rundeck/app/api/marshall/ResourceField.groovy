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

package com.dtolabs.rundeck.app.api.marshall

/**
 * Annotation information for a field in a resource
 */
class ResourceField {
    String name
    /**
     * Minimum API version for the field to be included
     */
    int apiVersionMin = 1
    /**
     * Maximum API version for the field to be included
     */
    int apiVersionMax = -1
    /**
     * The field should be ignored
     */
    boolean ignore
    /**
     * The field should be ignored in marshalling if its value is null
     */
    boolean ignoreOnlyIfNull
    /**
     * The name to use for this field as an XML attribute
     */
    String xmlAttr
    /**
     * True if the field should be marshalled as a direct subelement of the parent XML element,
     * instead of within an element using the field name.
     */
    boolean subElement
    /**
     * The name of the element to marshall for the value of this field, instead of the field name.
     */
    String elementName
    /**
     * The name of the element to marshall for the value of each item in a collection, instead of the default
     */
    String collectionKeyName

    String customFormat
}