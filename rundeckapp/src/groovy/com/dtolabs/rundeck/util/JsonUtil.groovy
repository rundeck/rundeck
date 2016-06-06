package com.dtolabs.rundeck.util

import org.codehaus.groovy.grails.web.json.JSONObject

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
        JSONObject.NULL == data ? null : data
    }
}
