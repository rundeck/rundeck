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

/*
* PropertyUtil.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/27/11 6:02 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;



/**
 * PropertyUtil factory for specific property types
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PropertyUtil {
    /**
     *
     * @param type type
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param values optional values list
     * @return a property instance for a particular simple type
     */
    public static Property forType(final Property.Type type, final String name, final String title,
                                   final String description, final boolean required,
                                   final String defaultValue, final List<String> values) {
        return forType(type, name, title, description, required, defaultValue, values, null);
    }

    /**
     * @param type type
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param values optional values list
     * @param validator validator
     * @return a property instance for a particular simple type
     */
    public static Property forType(final Property.Type type,
                                   final String name,
                                   final String title,
                                   final String description,
                                   final boolean required,
                                   final String defaultValue,
                                   final List<String> values,
                                   final PropertyValidator validator) {
        return forType(type, name, title, description, required, defaultValue, values, validator, null);
    }

    /**
     * @param type type
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param values optional values list
     * @param validator validator
     * @param scope resolution scope
     * @return a property instance for a particular simple type
     */
    public static Property forType(final Property.Type type,
                                   final String name,
                                   final String title,
                                   final String description,
                                   final boolean required,
                                   final String defaultValue,
                                   final List<String> values,
                                   final PropertyValidator validator,
                                   final PropertyScope scope) {
        return forType(type, name, title, description, required, defaultValue, values, validator, scope, null);
    }

    /**
     * @param type             type
     * @param name             name
     * @param title            optional title
     * @param description      optional description
     * @param required         true if required
     * @param defaultValue     optional default value
     * @param values           optional values list
     * @param validator        validator
     * @param scope            resolution scope
     * @param renderingOptions options
     *
     * @return a property instance for a particular simple type
     */
    public static Property forType(
            final Property.Type type,
            final String name,
            final String title,
            final String description,
            final boolean required,
            final String defaultValue,
            final List<String> values,
            final PropertyValidator validator,
            final PropertyScope scope,
            final Map<String, Object> renderingOptions
    )
    {
        return forType(
                type,
                name,
                title,
                description,
                required,
                defaultValue,
                values,
                null,
                validator,
                scope,
                renderingOptions
        );
    }
    /**
     * @param type type
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param values optional values list
     * @param validator validator
     * @param scope resolution scope
     * @param renderingOptions options
     * @return a property instance for a particular simple type
     */
    public static Property forType(final Property.Type type,
                                   final String name,
                                   final String title,
                                   final String description,
                                   final boolean required,
                                   final String defaultValue,
                                   final List<String> values,
                                   final Map<String, String> labels,
                                   final PropertyValidator validator,
                                   final PropertyScope scope,
                                   final Map<String, Object> renderingOptions
    ) {
        switch (type) {
            case Integer:
                return integer(name, title, description, required, defaultValue, validator, scope, renderingOptions);
            case Boolean:
                return bool(name, title, description, required, defaultValue, scope, renderingOptions);
            case Long:
                return longProp(name, title, description, required, defaultValue, validator, scope, renderingOptions);
            case Select:
                return PropertyUtil.select(
                        name,
                        title,
                        description,
                        required,
                        defaultValue,
                        values,
                        labels,
                        scope,
                        renderingOptions
                );
            case FreeSelect:
                return PropertyUtil.freeSelect(name,
                                               title,
                                               description,
                                               required,
                                               defaultValue,
                                               values,
                                               labels,
                                               validator,
                                               scope, renderingOptions);
            case Options:
                return PropertyUtil.options(
                        name,
                        title,
                        description,
                        required,
                        defaultValue,
                        values,
                        labels,
                        scope,
                        renderingOptions
                );
            default:
                return string(name, title, description, required, defaultValue, validator, scope, renderingOptions);
        }
    }


    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @return a string property
     */
    public static Property string(final String name, final String title, final String description,
                                  final boolean required,
                                  final String defaultValue) {
        return string(name, title, description, required, defaultValue, null);
    }

    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param validator validator
     * @return  a string property
     */
    public static Property string(final String name, final String title, final String description,
                                  final boolean required,
                                  final String defaultValue, final PropertyValidator validator) {
        return string(name, title, description, required, defaultValue, validator, null);
    }
    
    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param validator validator
     * @param scope resolution scope
     * @return Return a string property
     */
    public static Property string(final String name, final String title, final String description,
                                  final boolean required,
                                  final String defaultValue, final PropertyValidator validator,
                                  final PropertyScope scope) {
        return string(name, title, description, required, defaultValue, validator, scope, null);
    }

    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param validator validator
     * @param scope resolution scope
     * @param renderingOptions options
     * @return Return a string property
     */
    public static Property string(final String name, final String title, final String description,
                                  final boolean required,
                                  final String defaultValue, final PropertyValidator validator,
                                  final PropertyScope scope, final Map<String, Object> renderingOptions) {
        return new StringProperty(name, title, description, required, defaultValue, validator, scope, renderingOptions);
    }

    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @return Return a boolean property
     */
    public static Property bool(final String name, final String title, final String description, final boolean required,
                                final String defaultValue) {
        return bool(name, title, description, required, defaultValue, null);
    }
    
    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param scope resolution scope
     * @return Return a boolean property
     */
    public static BooleanProperty bool(String name,
                                       String title,
                                       String description,
                                       boolean required,
                                       String defaultValue, final PropertyScope scope) {
        return bool(name, title, description, required, defaultValue, scope, null);
    }

    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param scope resolution scope
     * @param renderingOptions options
     * @return  a boolean property
     */
    public static BooleanProperty bool(String name,
                                       String title,
                                       String description,
                                       boolean required,
                                       String defaultValue, final PropertyScope scope, 
                                       final Map<String, Object> renderingOptions) {
        return new BooleanProperty(name, title, description, required, defaultValue, scope, renderingOptions);
    }

    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @return  an integer property
     */
    public static Property integer(final String name, final String title, final String description,
                                   final boolean required,
                                   final String defaultValue) {
        return integer(name, title, description, required, defaultValue, null);

    }

    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param validator validator
     * @return  an integer property with additional validator
     */
    public static Property integer(final String name, final String title, final String description,
                                   final boolean required,
                                   final String defaultValue, final PropertyValidator validator) {

        return integer(name, title, description, required, defaultValue, validator, null);
    }
    
    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param validator validator
     * @param scope resolution scope
     * @return  an integer property with additional validator
     */
    public static Property integer(final String name,
                                   final String title,
                                   final String description,
                                   final boolean required,
                                   final String defaultValue,
                                   final PropertyValidator validator,
                                   final PropertyScope scope) {
        return integer(name, title, description, required, defaultValue, validator, scope, null);
    }

    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param validator validator
     * @param scope resolution scope
     * @param renderingOptions options
     * @return  an integer property with additional validator
     */
    public static Property integer(final String name,
                                   final String title,
                                   final String description,
                                   final boolean required,
                                   final String defaultValue,
                                   final PropertyValidator validator,
                                   final PropertyScope scope,
                                   final Map<String, Object> renderingOptions) {

        return new IntegerProperty(name, title, description, required, defaultValue, validator, scope, renderingOptions);
    }

    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @return  a long property
     */
    public static Property longProp(final String name, final String title, final String description,
                                    final boolean required, final String defaultValue) {
        return longProp(name, title, description, required, defaultValue, null);
    }

    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param validator validator
     * @return  a long property
     */
    public static Property longProp(final String name,
                                    final String title,
                                    final String description,
                                    final boolean required,
                                    final String defaultValue,
                                    final PropertyValidator validator) {
        return longProp(name, title, description, required, defaultValue, validator, null);
    }
    
    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param validator validator
     * @param scope resolution scope
     * @return  a long property
     */
    public static Property longProp(final String name,
                                    final String title,
                                    final String description,
                                    final boolean required,
                                    final String defaultValue,
                                    final PropertyValidator validator,
                                    final PropertyScope scope) {
        return longProp(name, title, description, required, defaultValue, validator, scope, null);
    }

    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param validator validator
     * @param scope resolution scope
     * @param renderingOptions options
     * @return  a long property
     */
    public static Property longProp(final String name,
                                    final String title,
                                    final String description,
                                    final boolean required,
                                    final String defaultValue,
                                    final PropertyValidator validator,
                                    final PropertyScope scope,
                                    final Map<String, Object> renderingOptions) {
        return new LongProperty(name, title, description, required, defaultValue, validator, scope, renderingOptions);
    }


    /**
     *
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param selectValues optional values list
     * @return  a Select property with a list of values
     */
    public static Property select(final String name, final String title, final String description,
                                  final boolean required, final String defaultValue, final List<String> selectValues) {
        return select(name, title, description, required, defaultValue, selectValues, null);
    }

    /**
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param selectValues optional values list
     * @param scope resolution scope
     * @return a Select property with a list of values
     */
    public static Property select(final String name, final String title, final String description,
                                  final boolean required, final String defaultValue, final List<String> selectValues,
                                  final PropertyScope scope) {

        return select(name, title, description, required, defaultValue, selectValues, scope, null);
    }
    
    /**
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param selectValues optional values list
     * @param scope resolution scope
     * @param renderingOptions options
     * @return a Select property with a list of values
     */
    public static Property select(final String name, final String title, final String description,
                                  final boolean required, final String defaultValue, final List<String> selectValues,
                                  final PropertyScope scope, final Map<String, Object> renderingOptions
    )
    {

        return select(
                name,
                title,
                description,
                required,
                defaultValue,
                selectValues,
                null,
                scope,
                renderingOptions
        );
    }

    /**
     * @param name             name
     * @param title            optional title
     * @param description      optional description
     * @param required         true if required
     * @param defaultValue     optional default value
     * @param selectValues     optional values list
     * @param scope            resolution scope
     * @param renderingOptions options
     *
     * @return a Select property with a list of values
     */
    public static Property select(
            final String name, final String title, final String description,
            final boolean required, final String defaultValue, final List<String> selectValues,
            final Map<String, String> selectLabels,
            final PropertyScope scope, final Map<String, Object> renderingOptions
    )
    {

        return new SelectProperty(
                name,
                title,
                description,
                required,
                defaultValue,
                selectValues,
                selectLabels,
                scope,
                renderingOptions
        );
    }
    /**
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param selectValues optional values list
     * @param scope resolution scope
     * @param renderingOptions options
     * @return a Select property with a list of values
     */
    public static Property select(final String name, final String title, final String description,
                                  final boolean required, final String defaultValue, final Collection<? extends
            Enum<?>> selectValues,

                                  final PropertyScope scope,
                                  final Map<String, Object> renderingOptions
    )
    {
        return select(name, title, description, required, defaultValue, selectValues, null, scope, renderingOptions);
    }

    /**
     * @param name             name
     * @param title            optional title
     * @param description      optional description
     * @param required         true if required
     * @param defaultValue     optional default value
     * @param selectValues     optional values list
     * @param scope            resolution scope
     * @param renderingOptions options
     *
     * @return a Select property with a list of values
     */
    public static Property select(
            final String name, final String title, final String description,
            final boolean required, final String defaultValue, final Collection<? extends Enum<?>> selectValues,
            final Map<String, String> selectLabels,
            final PropertyScope scope, final Map<String, Object> renderingOptions
    )
    {
        //create string representation of the enum values
        ArrayList<String> strings = new ArrayList<String>();
        for (Enum<?> selectValue : selectValues) {
            strings.add(selectValue.name());
        }
        return new SelectProperty(name, title, description, required, defaultValue, strings, selectLabels, scope,
                                  renderingOptions
        );
    }

    /**
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param selectValues optional values list
     * @return a Free Select property with a list of values
     */
    public static Property freeSelect(final String name, final String title, final String description,
                                      final boolean required, final String defaultValue,
                                      final List<String> selectValues) {
        return freeSelect(name, title, description, required, defaultValue, selectValues, null);
    }

    /**
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param selectValues optional values list
     * @param validator validator
     * @return a Free Select property with a list of values
     */
    public static Property freeSelect(final String name, final String title, final String description,
                                      final boolean required, final String defaultValue,
                                      final List<String> selectValues, final PropertyValidator validator) {

        return freeSelect(name, title, description, required, defaultValue, selectValues, validator, null);
    }
    
    /**
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param selectValues optional values list
     * @param validator validator
     * @param scope resolution scope
     * @return a Free Select property with a list of values
     */
    public static Property freeSelect(final String name, final String title, final String description,
                                      final boolean required, final String defaultValue,
                                      final List<String> selectValues, final PropertyValidator validator,
                                      final PropertyScope scope) {

        return freeSelect(name, title, description, required, defaultValue, selectValues, validator, scope, null);
    }

    /**
     * @param name name
     * @param title optional title
     * @param description optional description
     * @param required true if required
     * @param defaultValue optional default value
     * @param selectValues optional values list
     * @param validator validator
     * @param scope resolution scope
     * @param renderingOptions options
     * @return a Free Select property with a list of values
     */
    public static Property freeSelect(final String name, final String title, final String description,
                                      final boolean required, final String defaultValue,
                                      final List<String> selectValues, final PropertyValidator validator,
                                      final PropertyScope scope, final Map<String, Object> renderingOptions) {

        return new FreeSelectProperty(
                name,
                title,
                description,
                required,
                defaultValue,
                selectValues,
                null,
                validator,
                scope,
                renderingOptions
        );
    }

    /**
     * @param name             name
     * @param title            optional title
     * @param description      optional description
     * @param required         true if required
     * @param defaultValue     optional default value
     * @param selectValues     optional values list
     * @param validator        validator
     * @param scope            resolution scope
     * @param renderingOptions options
     *
     * @return a Free Select property with a list of values
     */
    public static Property freeSelect(
            final String name, final String title, final String description,
            final boolean required, final String defaultValue,
            final List<String> selectValues,
            final Map<String, String> labels,
            final PropertyValidator validator,
            final PropertyScope scope, final Map<String, Object> renderingOptions
    )
    {

        return new FreeSelectProperty(
                name,
                title,
                description,
                required,
                defaultValue,
                selectValues,
                labels,
                validator,
                scope,
                renderingOptions
        );
    }

    /**
     * @param name             name
     * @param title            optional title
     * @param description      optional description
     * @param required         true if required
     * @param defaultValue     optional default value
     * @param selectValues     optional values list
     * @param scope            resolution scope
     * @param renderingOptions options
     *
     * @return a Free Select property with a list of values
     */
    public static Property options(
            final String name,
            final String title,
            final String description,
            final boolean required,
            final String defaultValue,
            final List<String> selectValues,
            final Map<String, String> labels,
            final PropertyScope scope,
            final Map<String, Object> renderingOptions
    )
    {

        return new OptionsProperty(
                name,
                title,
                description,
                required,
                defaultValue,
                selectValues,
                labels,
                scope,
                renderingOptions
        );
    }

    static final class StringProperty extends PropertyBase {

        public StringProperty(final String name,
                              final String title,
                              final String description,
                              final boolean required,
                              final String defaultValue,
                              final PropertyValidator validator,
                              final PropertyScope scope,
                              final Map<String, Object> renderingOptions) {
            super(name, title, description, required, defaultValue, validator, scope, renderingOptions);
        }

        public Type getType() {
            return Type.String;
        }
    }

    static PropertyValidator andValidator(final PropertyValidator first, final PropertyValidator second) {
        if (null == first) {
            return second;
        } else if (null == second) {
            return first;
        }
        return value -> first.isValid(value) && second.isValid(value);
    }

    static class FreeSelectProperty extends PropertyBase {
        final List<String> selectValues;
        final Map<String, String> selectLabels;

        public FreeSelectProperty(final String name,
                                  final String title,
                                  final String description,
                                  final boolean required,
                                  final String defaultValue,
                                  final List<String> selectValues,
                                  final Map<String, String> selectLabels,
                                  final PropertyValidator validator,
                                  final PropertyScope scope,
                                  final Map<String, Object> renderingOptions
        )
        {
            super(name, title, description, required, defaultValue, validator, scope, renderingOptions);
            this.selectValues = selectValues;
            this.selectLabels = selectLabels;
        }

        public Type getType() {
            return Type.FreeSelect;
        }

        @Override
        public List<String> getSelectValues() {
            return selectValues;
        }

        @Override
        public Map<String, String> getSelectLabels() {
            return selectLabels;
        }
    }

    static final class SelectProperty extends FreeSelectProperty {

        public SelectProperty(
                final String name,
                final String title,
                final String description,
                final boolean required,
                final String defaultValue,
                final List<String> selectValues,
                final Map<String, String> selectLabels,
                final PropertyScope scope,
                final Map<String, Object> renderingOptions
        )
        {
            super(
                    name,
                    title,
                    description,
                    required,
                    defaultValue,
                    selectValues,
                    selectLabels,
                    new SelectValidator(selectValues),
                    scope,
                    renderingOptions
            );
        }

        public Type getType() {
            return Type.Select;
        }
    }

    static final class OptionsProperty extends FreeSelectProperty {

        public OptionsProperty(
                final String name,
                final String title,
                final String description,
                final boolean required,
                final String defaultValue,
                final List<String> selectValues,
                final Map<String, String> selectLabels,
                final PropertyScope scope,
                final Map<String, Object> renderingOptions
        )
        {
            super(
                    name,
                    title,
                    description,
                    required,
                    defaultValue,
                    selectValues,
                    selectLabels,
                    new OptionsValidator(selectValues),
                    scope,
                    renderingOptions
            );
        }

        public Type getType() {
            return Type.Options;
        }
    }

    static final class SelectValidator implements PropertyValidator {

        final List<String> selectValues;

        SelectValidator(final List<String> selectValues) {
            this.selectValues = selectValues;
        }

        public boolean isValid(final String value) throws ValidationException {
            return selectValues.contains(value);
        }
    }

    static final class OptionsValidator implements PropertyValidator {

        final List<String> selectValues;

        OptionsValidator(final List<String> selectValues) {
            this.selectValues = selectValues;
        }

        public boolean isValid(final String value) throws ValidationException {
            Set<String> propvalset = new HashSet<>();
            if (value.indexOf(',') > 0) {
                Stream<String> stream = Arrays.stream(value.split(", *"));
                propvalset.addAll(stream.map(new Function<java.lang.String, java.lang.String>() {
                    @Override
                    public String apply(final String s1) {
                        return s1.trim();
                    }
                }).filter(new Predicate<java.lang.String>() {
                    @Override
                    public boolean test(final String s) {
                        return !"".equals(s);
                    }
                }).collect(Collectors.toSet()));
            } else {
                propvalset.add(value);
            }
            return selectValues.containsAll(propvalset);
        }
    }

    static final class BooleanProperty extends PropertyBase {
        public BooleanProperty(final String name, final String title, final String description, final boolean required,
                               final String defaultValue, final PropertyScope scope, Map<String, Object> renderingOptions) {
            super(name, title, description, required, defaultValue, booleanValidator, scope, renderingOptions);
        }

        public Type getType() {
            return Type.Boolean;
        }
    }

    static final PropertyValidator booleanValidator = value -> "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);

    static final class IntegerProperty extends PropertyBase {
        public IntegerProperty(final String name, final String title, final String description, final boolean required,
                               final String defaultValue, final PropertyValidator validator, final PropertyScope scope, 
                               final Map<String, Object> renderingOptions) {
            super(name, title, description, required, defaultValue, andValidator(integerValidator, validator), scope, renderingOptions);
        }

        public Type getType() {
            return Type.Integer;
        }

    }

    static final PropertyValidator integerValidator = value -> {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ValidationException("Not a valid integer");
        }
        return true;
    };

    static final class LongProperty extends PropertyBase {
        public LongProperty(final String name, final String title, final String description, final boolean required,
                            final String defaultValue, final PropertyValidator validator, final PropertyScope scope,
                            final Map<String, Object> renderingOptions) {
            super(name, title, description, required, defaultValue, andValidator(longValidator, validator), scope, renderingOptions);
        }

        public Type getType() {
            return Type.Long;
        }

    }

    static final PropertyValidator longValidator = value -> {
        try {
            Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ValidationException("Not a valid integer");
        }
        return true;
    };

}
