package com.dtolabs.rundeck.plugins.util;

import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil;

import java.util.Arrays;
import java.util.List;


/**
 * A builder for Property objects
 */
public class PropertyBuilder {
    private Property.Type type;
    private String name;
    private String title;
    private String description;
    private boolean required;
    private String value;
    private List<String> values;
    private Property.Validator validator;

    private PropertyBuilder() {

    }

    public static PropertyBuilder builder() {
        return new PropertyBuilder();
    }

    public PropertyBuilder string(final String name) {
        name(name);
        type(Property.Type.String);
        return this;
    }

    public PropertyBuilder booleanType(final String name) {
        name(name);
        type(Property.Type.Boolean);
        return this;
    }

    public PropertyBuilder integer(final String name) {
        name(name);
        type(Property.Type.Integer);
        return this;
    }

    public PropertyBuilder longType(final String name) {
        name(name);
        type(Property.Type.Long);
        return this;
    }

    public PropertyBuilder select(final String name) {
        name(name);
        type(Property.Type.Select);
        return this;
    }

    public PropertyBuilder freeSelect(final String name) {
        name(name);
        type(Property.Type.FreeSelect);
        return this;
    }

    public PropertyBuilder type(final Property.Type type) {
        this.type = type;
        return this;
    }

    public PropertyBuilder name(final String name) {
        this.name = name;
        return this;
    }

    public PropertyBuilder title(final String title) {
        this.title = title;
        return this;
    }

    public PropertyBuilder description(final String description) {
        this.description = description;
        return this;
    }

    public PropertyBuilder required(final boolean required) {
        this.required = required;
        return this;
    }

    public PropertyBuilder defaultValue(final String value) {
        this.value = value;
        return this;
    }

    public PropertyBuilder values(final List<String> values) {
        this.values = values;
        return this;
    }
    public PropertyBuilder values(final String... values) {
        this.values = Arrays.asList(values);
        return this;
    }
    public PropertyBuilder validator(final Property.Validator validator) {
        this.validator = validator;
        return this;
    }

    public Property build() {
        if (null == type) {
            throw new IllegalStateException("type is required");
        }
        if (null == name) {
            throw new IllegalStateException("name is required");
        }
        return PropertyUtil.forType(type, name, title, description, required, value, values);
    }
}