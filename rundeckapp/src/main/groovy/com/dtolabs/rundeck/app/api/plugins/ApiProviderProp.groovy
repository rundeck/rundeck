package com.dtolabs.rundeck.app.api.plugins

import groovy.transform.CompileStatic

@CompileStatic
class ApiProviderProp {
    String name
    String desc
    String title
    String defaultValue
    String staticTextDefaultValue
    boolean required
    String type
    List<String> allowed
    Map<String, String> selectLabels
    String scope
    Map<String, String> options

    static ApiProviderProp from(Map<String, Object> values) {
        def prop = new ApiProviderProp()
        prop.name = values.get("name")?.toString()
        prop.desc = values.get("desc")?.toString()
        prop.title = values.get("title")?.toString()
        prop.defaultValue = values.get("defaultValue")?.toString()
        prop.staticTextDefaultValue = values.get("staticTextDefaultValue")?.toString()
        prop.required = values.get("required") ? true : false
        prop.type = values.get("type")?.toString()
        if (values.get('allowed') instanceof List) {
            prop.allowed = (List<String>) values.get('allowed')
        }
        if (values.get('selectLabels') instanceof Map) {
            prop.selectLabels = (Map<String, String>) values.get('selectLabels')
        }
        prop.scope = values.get("scope")?.toString()
        if (values.get('options') instanceof Map) {
            prop.options = (Map<String, String>) values.get('options')
        }
        return prop

    }
}
