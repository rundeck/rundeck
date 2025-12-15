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

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import org.grails.web.json.JSONObject


/**
 * JSON request handling utilities
 */
class JsonUtil {

    // Grails 7: Shared ObjectMapper instance for parsing JSON request bodies
    private static final ObjectMapper objectMapper = new ObjectMapper()

    /**
     * Grails 7: Parse JSON from HttpServletRequest body using Jackson ObjectMapper.
     * This replaces the broken request.JSON property in Grails 7/Spring Boot 3.
     * 
     * @param request HttpServletRequest with JSON body
     * @return Parsed JSON as Map, or null if request body is empty or already consumed
     * @throws IOException if JSON parsing fails (but not if body is unavailable)
     */
    static Map parseRequestBody(HttpServletRequest request) throws IOException {
        // Grails 7: Check if this is actually a JSON request
        def contentType = request.contentType
        if (contentType) {
            // Skip non-JSON content types (multipart/form-data, etc.)
            if (!contentType.toLowerCase().contains('json')) {
                return null
            }
        }
        
        // Spring Boot 3: Try multiple approaches to read the request body
        
        // Attempt 1: Read from input stream
        try {
            def inputStream = request.getInputStream()
            if (inputStream && inputStream.available() > 0) {
                return objectMapper.readValue(inputStream, Map.class)
            }
        } catch (Exception e) {
            // Stream might be already consumed or unavailable
            // Continue to next attempt
        }
        
        // Attempt 2: Read from reader  
        try {
            def reader = request.getReader()
            if (reader) {
                def content = reader.text
                if (content && !content.trim().empty) {
                    return objectMapper.readValue(content, Map.class)
                }
            }
        } catch (Exception e) {
            // Reader might be unavailable
            // Continue to next attempt
        }
        
        // NOTE: No fallback needed for Grails 7 controllers using @RequestBody
        // Spring automatically handles JSON deserialization for @RequestBody parameters
        
        return null
    }

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
