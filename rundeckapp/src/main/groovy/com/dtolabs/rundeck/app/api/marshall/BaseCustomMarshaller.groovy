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
 * Base for json/xml custom marshaller
 */
class BaseCustomMarshaller {
    ApiMarshaller marshaller
    int apiVersion

    BaseCustomMarshaller(ApiMarshaller marshaller, int apiVersion) {
        this.marshaller = marshaller
        this.apiVersion = apiVersion
    }

    boolean supports(final Object object) {
        return marshaller.supports(object)
    }

    Object formatCustom(final Object object, String format) {
        if (object instanceof CustomFormatter) {
            ((CustomFormatter) object).format(format)
        } else {
            object
        }
    }
    /**
     * Scan fields of the object based on the ApiMarshaller introspection, and return
     * the maps to use for XML Attributes, fields, and direct subelements.
     * @param object
     * @return [attrs, fields, subelements]
     */
    protected List introspect(object) {
        //determine attributes and fields to included
        def attrs = [:]
        def fields = [:]
        def subelements = [:]
        def collectionElemNames = [:]
        def customFormat = [:]
        def fieldDefs = marshaller.getFieldDefsForClass(object.class)

        object.properties.keySet().each { String k ->
            ResourceField resField = fieldDefs[k]
            if (!resField) {
                return
            }
            if (resField.apiVersionMin > 1 && apiVersion < resField.apiVersionMin) {
                return
            }
            if (resField.apiVersionMax > 0 && apiVersion > resField.apiVersionMax) {
                return
            }

            if (resField.ignore) {
                return
            }
            if (resField.ignoreOnlyIfNull && null == object[k]) {
                return
            }
            def fname = k
            def used = false
            if (resField.xmlAttr) {
                fname = resField.xmlAttr
                attrs[fname] = object[k]
                used = true
            } else if (resField.subElement) {
                subelements[k] = object[k]
                used = true
            } else if (resField.elementName) {
                fname = resField.elementName
            }
            if (!used) {
                fields[fname] = object[k]
            }

            if (resField.collectionKeyName) {
                collectionElemNames[fname] = resField.collectionKeyName
            }

            if (resField.customFormat) {
                customFormat[fname] = resField.customFormat
            }
        }
        [attrs, fields, subelements, collectionElemNames, customFormat]
    }
}
