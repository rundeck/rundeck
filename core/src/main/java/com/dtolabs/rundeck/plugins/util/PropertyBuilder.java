package com.dtolabs.rundeck.plugins.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dtolabs.rundeck.core.plugins.configuration.Property;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyUtil;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator;


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
    private PropertyValidator validator;
    private PropertyScope scope;
    private Map<String, Object> renderingOptions = new HashMap<String, Object>();

    private PropertyBuilder() {

    }

    public static PropertyBuilder builder() {
        return new PropertyBuilder();
    }

    /**
     * Return a new instance preconfigured with a previously defined Property
     */
    public static PropertyBuilder builder(final Property orig) {
        return new PropertyBuilder()
            .name(orig.getName())
            .defaultValue(orig.getDefaultValue())
            .required(orig.isRequired())
            .type(orig.getType())
            .description(orig.getDescription())
            .title(orig.getTitle())
            .values(orig.getSelectValues())
            .validator(orig.getValidator())
            .scope(orig.getScope())
            .renderingOptions(orig.getRenderingOptions())
            ;
    }

    /**
     * Return a new PropertyBuilder of type {@link Property.Type.String}
     */
    public PropertyBuilder string(final String name) {
        name(name);
        type(Property.Type.String);
        return this;
    }

    /**
     * Return a new PropertyBuilder of type {@link Property.Type.Boolean}
     */
    public PropertyBuilder booleanType(final String name) {
        name(name);
        type(Property.Type.Boolean);
        return this;
    }

    /**
     * Return a new PropertyBuilder of type {@link Property.Type.Integer}
     */
    public PropertyBuilder integer(final String name) {
        name(name);
        type(Property.Type.Integer);
        return this;
    }

    /**
     * Return a new PropertyBuilder of type {@link Property.Type.Long}
     */
    public PropertyBuilder longType(final String name) {
        name(name);
        type(Property.Type.Long);
        return this;
    }

    /**
     * Return a new PropertyBuilder of type {@link Property.Type.Select}
     */
    public PropertyBuilder select(final String name) {
        name(name);
        type(Property.Type.Select);
        return this;
    }

    /**
     * Return a new PropertyBuilder of type {@link Property.Type.FreeSelect}
     */
    public PropertyBuilder freeSelect(final String name) {
        name(name);
        type(Property.Type.FreeSelect);
        return this;
    }

    /**
     * Set the type
     */
    public PropertyBuilder type(final Property.Type type) {
        this.type = type;
        return this;
    }

    /**
     * Set the name (identifier)
     */
    public PropertyBuilder name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the title (display name)
     */
    public PropertyBuilder title(final String title) {
        this.title = title;
        return this;
    }

    /**
     * Set the description
     */
    public PropertyBuilder description(final String description) {
        this.description = description;
        return this;
    }

    /**
     * Set required
     */
    public PropertyBuilder required(final boolean required) {
        this.required = required;
        return this;
    }

    /**
     * Set the default value
     */
    public PropertyBuilder defaultValue(final String value) {
        this.value = value;
        return this;
    }

    /**
     * Set the select values
     */
    public PropertyBuilder values(final List<String> values) {
        this.values = values;
        return this;
    }

    /**
     * Set the select values
     */
    public PropertyBuilder values(final String... values) {
        this.values = Arrays.asList(values);
        return this;
    }

    /**
     * Set the property validator, only applies to String, Integer, Long, and FreeSelect types
     */
    public PropertyBuilder validator(final PropertyValidator validator) {
        this.validator = validator;
        return this;
    }

    /**
     * Set the property scope
     */
    public PropertyBuilder scope(final PropertyScope scope) {
        this.scope = scope;
        return this;
    }
    
    /**
     * Adds all rendering options from the given renderingOptions
     */
    public PropertyBuilder renderingOptions(final Map<String, Object> renderingOptions) {
        this.renderingOptions.putAll(renderingOptions);
        return this;
    }
    
    /**
     * Adds the given renderingOption
     */
    public PropertyBuilder renderingOption(final String optionKey, final Object optionValue) {
        this.renderingOptions.put(optionKey, optionValue);
        return this;
    }

    /**
     * Build the Property object
     * @throws IllegalStateException if type or name is not set
     */
    public Property build() {
        if (null == type) {
            throw new IllegalStateException("type is required");
        }
        if (null == name) {
            throw new IllegalStateException("name is required");
        }
        return PropertyUtil.forType(type, name, title, description, required, value, values, validator, scope, renderingOptions);
    }

    /**
     * Return the type already defined for the builder
     * @return
     */
    public Property.Type getType() {
        return type;
    }
}
