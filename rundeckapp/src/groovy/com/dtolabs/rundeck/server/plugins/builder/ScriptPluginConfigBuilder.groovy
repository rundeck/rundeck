package com.dtolabs.rundeck.server.plugins.builder

import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder
import org.codehaus.groovy.runtime.InvokerHelper

/**
 * DSL for defining configuration properties
 */
class ScriptPluginConfigBuilder {
    private DescriptionBuilder descriptionBuilder1

    ScriptPluginConfigBuilder(DescriptionBuilder descriptionBuilder1) {
        this.descriptionBuilder1 = descriptionBuilder1
    }

    def propertyMissing(String property, Object newValue) {
        if (newValue instanceof Map) {
            buildProperty(property, newValue)
        } else if (newValue instanceof Integer || newValue instanceof String || newValue instanceof Long
                || newValue instanceof Boolean ) {
            buildProperty(property, [defaultValue: newValue])
        } else if (newValue instanceof List ) {
            buildProperty(property, [values: newValue])
        }else {
            super.setProperty(property, newValue)
        }
    }

    private buildProperty(String name, Map props, Closure validationClosure = null) {
        //default to string type
        def PropertyBuilder pbuilder = descriptionBuilder1.property(name)
        if(!pbuilder.getType()){
            pbuilder.type(Property.Type.String)
        }
        if (props['title']) {
            pbuilder.title(props['title'])
        }
        if (props['description']) {
            pbuilder.description(props['description'])
        }
        if (props['defaultValue'] != null) {
            //if no 'type' is defined for the property, guess it based on the default value
            if (props['type'] == null && !pbuilder.getType()) {
                if (props['defaultValue'] instanceof Integer) {
                    pbuilder.type(Property.Type.Integer)
                } else if (props['defaultValue'] instanceof Long) {
                    pbuilder.type(Property.Type.Long)
                } else if (props['defaultValue'] instanceof Boolean) {
                    pbuilder.type(Property.Type.Boolean)
                } else if (props['defaultValue'] instanceof List) {
                    pbuilder.type(Property.Type.Select)
                    pbuilder.values((List) props['defaultValue'])
                }else if (props['defaultValue'] instanceof String) {
                    pbuilder.type(Property.Type.String)
                    //if default string value is multi-line, use multi-line option
                    String defaultString = props['defaultValue']
                    if(defaultString.indexOf(System.getProperty("line.separator"))>=0){
                        props.multiline=true
                    }
                }
            }
            if (!(props['defaultValue'] instanceof List)) {
                pbuilder.defaultValue(props['defaultValue'].toString())
            }
        }
        if (props['required'] != null) {
            pbuilder.required(props['required'] ? true : false)
        }
        if (props['values'] != null) {
            pbuilder.values(InvokerHelper.asList(props['values']))
            if (props['type'] == null) {
                //if no 'type' is defined, set to Select, unless required is not set to true
                if (!props['required']) {
                    pbuilder.type(Property.Type.FreeSelect)
                } else {
                    pbuilder.type(Property.Type.Select)
                }
            }
        }
        if (props['type']) {
            if (props['type'] instanceof String) {
                pbuilder.type(Property.Type.valueOf(props['type']))
            } else if (props['type'] instanceof Property.Type) {
                pbuilder.type(props['type'])
            }
        }
        if (pbuilder.getType()==Property.Type.String && props['multiline']) {
            pbuilder.renderingOption(StringRenderingConstants.DISPLAY_TYPE_KEY,
                    StringRenderingConstants.DisplayType.MULTI_LINE)
        }
        if(props['renderingOptions'] instanceof Map){
            pbuilder.renderingOptions(props['renderingOptions'])
        }
        if(props['scope'] instanceof PropertyScope) {
            pbuilder.scope((PropertyScope) props['scope'])
        }else if(props['scope'] instanceof String){
            String scope = props['scope']
            pbuilder.scope(PropertyScope.valueOf(scope))
        }
        if (validationClosure != null) {
            //validation
            pbuilder.validator(new PropertyValidator() {
                @Override
                boolean isValid(String value) throws ValidationException {
                    //todo: support simpler closure results shortcuts for validation (string, null)
                    return validationClosure.call(value)
                }
            })
        }
        descriptionBuilder1.property(pbuilder)
    }

    def methodMissing(String name, args){
        List list = InvokerHelper.asList(args);
        if (list.size() ==1 && list[0] instanceof Map ) {
            buildProperty(name, (Map)list[0])
            return true
        }else if (list.size() == 2 && list[0] instanceof Map && list[1] instanceof Closure) {
            buildProperty(name, (Map)list[0], (Closure)list[1])
            return true
        } else {
            throw new MissingMethodException(name.toString(), getClass(), list.toArray(), false);
        }
    }
}
