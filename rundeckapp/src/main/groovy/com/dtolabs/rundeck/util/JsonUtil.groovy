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

package com.dtolabs.rundeck.util

import org.grails.web.json.JSONObject


/**
 * JSON request handling utilities
 */
class JsonUtil {

    /**
     * Validate a json object
     * @param data
     * @param spec
     * @return
     */
    static List<String> validateJson(data, Map<String, Object> spec, prefix='') {

        def errors = []
        spec.each { prop, Object type ->
            def required = prop.startsWith('!')
            prop = required?prop.substring(1):prop

            Object value = jsonNull(data[prop])
            if (value != null) {
                if (type instanceof Map) {
                    if (value instanceof Map) {
                        def newerrs = validateJson(value, type, "${prop}.")
                        errors.addAll(newerrs ?: [])
                    }else{
                        errors << "json: expected '${prefix}${prop}' to be a ${Map.simpleName}"
                    }
                } else if (type instanceof Class) {
                    if (!(type.isAssignableFrom(value.getClass()))) {
                        errors << "json: expected '${prefix}${prop}' to be a ${type.simpleName}"
                    }
                }
            }else if(required){
                errors<<"json: required '${prefix}${prop}' but it was not found"
            }
        }
        errors ?: null
    }
    /**
     * @param data JSONObject data
     * @return null if the data is null or Json NULL, the input value otherwise
     */
    static def jsonNull(data) {
        //JSONObject.NULL has been deprecated, should no longer be referenced and has been removed from Grails 3.1. Ref.: http://docs.grails.org/3.0.6/api/org/grails/web/json/JSONObject.Null.html
        null == data ? null : data
    }
}
