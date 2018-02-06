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
                renderingOptions,
                false
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
                                   final Map<String, Object> renderingOptions,
                                   final boolean dynamicValues
    ) {
        return forType(
                type,
                name,
                title,
                description,
                required,
                defaultValue,
                values,
                labels,
                validator,
                null,
                scope,
                renderingOptions,
                dynamicValues
        );
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
            final Map<String, String> labels,
            final PropertyValidator validator,
            final ValuesGenerator valuesGenerator,
            final PropertyScope scope,
            final Map<String, Object> renderingOptions,
            final boolean dynamicValues
    ) {
        return new PropertyBase(
                type,
                name,
                title,
                description,
                required,
                defaultValue,
                hasSelectValues(type) ? values : null,
                hasSelectValues(type) ? labels : null,
                typeValidatorFor(type, values, dynamicValues, validator),
                valuesGenerator,
                scope,
                renderingOptions
        );


    }

    private static boolean hasSelectValues(final Property.Type type) {
        switch (type) {
            case Options:
            case Map:
            case Select:
            case FreeSelect:
                return true;
        }
        return false;
    }

    private static PropertyValidator typeValidatorFor(
            final Property.Type type,
            final List<String> values,
            final boolean dynamicValues,
            final PropertyValidator validator
    ) {
        switch (type) {
            case Integer:
                return andValidator(integerValidator, validator);
            case Boolean:
                return booleanValidator;
            case Long:
                return andValidator(longValidator, validator);
            case Select:
                return new SelectValidator(values, dynamicValues);
            case Options:
                return new OptionsValidator(values);
            default:
                return validator;
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
        return forType(
                Property.Type.String,
                name,
                title,
                description,
                required,
                defaultValue,
                null,
                (Map<String, String>) null,
                validator,
                null,
                scope,
                renderingOptions,
                false
        );

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
    public static BooleanProperty bool(
            String name,
            String title,
            String description,
            boolean required,
            String defaultValue, final PropertyScope scope
    ) {
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
    public static BooleanProperty bool(
            String name,
            String title,
            String description,
            boolean required,
            String defaultValue, final PropertyScope scope,
            final Map<String, Object> renderingOptions
    ) {
        return new BooleanProperty(forType(
                Property.Type.Boolean,
                name,
                title,
                description,
                required,
                defaultValue,
                null,
                null,
                booleanValidator,
                null,
                scope,
                renderingOptions,
                false
        ));
    }

    /*
     * For binary compatibilty, this class remains as the return type of some bool methods
     */
    public static class BooleanProperty extends PropertyBase {
        public BooleanProperty(
                final Property prop
        ) {
            super(prop);
        }
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
        return forType(
                Property.Type.Integer,
                name,
                title,
                description,
                required,
                defaultValue,
                null,
                null,
                validator,
                null,
                scope,
                renderingOptions,
                false
        );

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
        return forType(
                Property.Type.Long,
                name,
                title,
                description,
                required,
                defaultValue,
                null,
                null,
                validator,
                null,
                scope,
                renderingOptions,
                false
        );

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
                renderingOptions,
                false
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
            final PropertyScope scope, final Map<String, Object> renderingOptions, final boolean dynamicValues
    )
    {

        return forType(
                Property.Type.Select,
                name,
                title,
                description,
                required,
                defaultValue,
                selectValues,
                selectLabels,
                null,
                null,
                scope,
                renderingOptions,
                dynamicValues
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


        return forType(
                Property.Type.Select,
                name,
                title,
                description,
                required,
                defaultValue,
                strings,
                selectLabels,
                null,
                null,
                scope,
                renderingOptions,
                false
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


        return forType(
                Property.Type.FreeSelect,
                name,
                title,
                description,
                required,
                defaultValue,
                selectValues,
                null,
                validator,
                null,
                scope,
                renderingOptions,
                false
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


        return forType(
                Property.Type.FreeSelect,
                name,
                title,
                description,
                required,
                defaultValue,
                selectValues,
                labels,
                validator,
                null,
                scope,
                renderingOptions,
                false
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
        return forType(
                Property.Type.Options,
                name,
                title,
                description,
                required,
                defaultValue,
                selectValues,
                labels,
                null,
                null,
                scope,
                renderingOptions,
                false
        );
    }

    /**
     * @param name             name
     * @param title            optional title
     * @param description      optional description
     * @param required         true if required
     * @param defaultValue     optional default value
     * @param validator        validator
     * @param scope            resolution scope
     * @param renderingOptions options
     * @return a Free Select property with a list of values
     */
    public static Property map(
            final String name,
            final String title,
            final String description,
            final boolean required,
            final String defaultValue,
            final PropertyValidator validator,
            final PropertyScope scope,
            final Map<String, Object> renderingOptions
    ) {

        return forType(
                Property.Type.Map,
                name,
                title,
                description,
                required,
                defaultValue,
                null,
                null,
                validator,
                null,
                scope,
                renderingOptions,
                false
        );
    }

    static PropertyValidator andValidator(final PropertyValidator first, final PropertyValidator second) {
        if (null == first) {
            return second;
        } else if (null == second) {
            return first;
        }
        return value -> first.isValid(value) && second.isValid(value);
    }

    static final class SelectValidator implements PropertyValidator {

        final List<String> selectValues;
        final boolean dynamicValues;

        SelectValidator(final List<String> selectValues, final boolean dynamicValues) {
            this.selectValues = selectValues;
            this.dynamicValues = dynamicValues;
        }

        public boolean isValid(final String value) throws ValidationException {
            //TODO: How validate this if is remote values?
            return dynamicValues || selectValues.contains(value);
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

    static final PropertyValidator booleanValidator = value -> "true".equalsIgnoreCase(value) || "false"
            .equalsIgnoreCase(value);

    static final PropertyValidator integerValidator = value -> {
        try {
            Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ValidationException("Not a valid integer");
        }
        return true;
    };


    static final PropertyValidator longValidator = value -> {
        try {
            Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new ValidationException("Not a valid integer");
        }
        return true;
    };

}
