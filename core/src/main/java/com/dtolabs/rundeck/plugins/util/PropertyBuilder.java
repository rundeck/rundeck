package com.dtolabs.rundeck.plugins.util;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dtolabs.rundeck.core.plugins.configuration.*;


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
     * @param orig original property
     *
     * @return this builder
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
     * Return a new PropertyBuilder of type {@link Property.Type#String}
     * @param name name
     *
     * @return this builder
     */
    public PropertyBuilder string(final String name) {
        name(name);
        type(Property.Type.String);
        return this;
    }

    /**
     * Return a new PropertyBuilder of type {@link Property.Type#Boolean}
     * @param name name
     *
     * @return this builder
     */
    public PropertyBuilder booleanType(final String name) {
        name(name);
        type(Property.Type.Boolean);
        return this;
    }

    /**
     * Return a new PropertyBuilder of type {@link Property.Type#Integer}
     * @param name name
     *
     * @return this builder
     */
    public PropertyBuilder integer(final String name) {
        name(name);
        type(Property.Type.Integer);
        return this;
    }

    /**
     * Return a new PropertyBuilder of type {@link Property.Type#Long}
     * @param name name
     *
     * @return this builder
     */
    public PropertyBuilder longType(final String name) {
        name(name);
        type(Property.Type.Long);
        return this;
    }

    /**
     * Return a new PropertyBuilder of type {@link Property.Type#Select}
     * @param name name
     *
     * @return this builder
     */
    public PropertyBuilder select(final String name) {
        name(name);
        type(Property.Type.Select);
        return this;
    }

    /**
     * Return a new PropertyBuilder of type {@link Property.Type#FreeSelect}
     * @param name name
     *
     * @return this builder
     */
    public PropertyBuilder freeSelect(final String name) {
        name(name);
        type(Property.Type.FreeSelect);
        return this;
    }

    /**
     * Set the type
     * @param type type
     *
     * @return this builder
     */
    public PropertyBuilder type(final Property.Type type) {
        this.type = type;
        return this;
    }

    /**
     * Set the name (identifier)
     * @param name name
     *
     * @return this builder
     */
    public PropertyBuilder name(final String name) {
        this.name = name;
        return this;
    }

    /**
     * Set the title (display name)
     * @param title title
     *
     * @return this builder
     */
    public PropertyBuilder title(final String title) {
        this.title = title;
        return this;
    }

    /**
     * Set the description
     * @param description description
     *
     * @return this builder
     */
    public PropertyBuilder description(final String description) {
        this.description = description;
        return this;
    }

    /**
     * Set required
     * @param required true if required
     *
     * @return this builder
     */
    public PropertyBuilder required(final boolean required) {
        this.required = required;
        return this;
    }

    /**
     * Set the default value
     * @param value value
     *
     * @return this builder
     */
    public PropertyBuilder defaultValue(final String value) {
        this.value = value;
        return this;
    }

    /**
     * Set the select values
     * @param values values
     *
     * @return this builder
     */
    public PropertyBuilder values(final List<String> values) {
        this.values = values;
        return this;
    }

    /**
     * Set the select values
     * @param values values
     *
     * @return this builder
     */
    public PropertyBuilder values(final String... values) {
        this.values = Arrays.asList(values);
        return this;
    }

    /**
     * Set the property validator, only applies to String, Integer, Long, and FreeSelect types
     * @param validator validator
     *
     * @return this builder
     */
    public PropertyBuilder validator(final PropertyValidator validator) {
        this.validator = validator;
        return this;
    }

    /**
     * Set the property scope
     * @param scope scope
     *
     * @return this builder
     */
    public PropertyBuilder scope(final PropertyScope scope) {
        this.scope = scope;
        return this;
    }
    
    /**
     * Adds all rendering options from the given renderingOptions
     * @param renderingOptions options
     *
     * @return this builder
     * @see StringRenderingConstants
     */
    public PropertyBuilder renderingOptions(final Map<String, Object> renderingOptions) {
        this.renderingOptions.putAll(renderingOptions);
        return this;
    }
    
    /**
     * Adds the given renderingOption
     * @param optionKey key
     * @param optionValue value
     *
     * @return this builder
     * @see StringRenderingConstants
     */
    public PropertyBuilder renderingOption(final String optionKey, final Object optionValue) {
        this.renderingOptions.put(optionKey, optionValue);
        return this;
    }
    /**
     * Set the string property to display as a Multi-line Text area.
     * @return this builder
     * @throws IllegalStateException if the property type is not {@link Property.Type#String}
     */
    public PropertyBuilder renderingAsTextarea() {
        if (this.type != Property.Type.String) {
            throw new IllegalStateException("stringRenderingTextarea can only be applied to a String property");
        }
        return renderingOption(StringRenderingConstants.DISPLAY_TYPE_KEY, StringRenderingConstants.DisplayType
                .MULTI_LINE);
    }
    /**
     * Set the string property to display as a Password.
     * @return this builder
     * @throws IllegalStateException if the property type is not {@link Property.Type#String}
     */
    public PropertyBuilder renderingAsPassword() {
        if (this.type != Property.Type.String) {
            throw new IllegalStateException("stringRenderingPassword can only be applied to a String property");
        }
        return renderingOption(StringRenderingConstants.DISPLAY_TYPE_KEY, StringRenderingConstants.DisplayType
                .PASSWORD);
    }

    /**
     * Build the Property object
     * @return built property
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
     * @return the type already defined for the builder
     */
    public Property.Type getType() {
        return type;
    }
}
