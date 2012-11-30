/*
 * Copyright 2011 DTO Solutions, Inc. (http://dtosolutions.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/*
* PropertyUtil.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 7/27/11 6:02 PM
* 
*/
package com.dtolabs.rundeck.core.plugins.configuration;

import java.util.List;

import static com.dtolabs.rundeck.core.plugins.configuration.Property.Type.*;


/**
 * PropertyUtil factory for specific property types
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PropertyUtil {
    /**
     * Return a property instance for a particular simple type
     */
    public static Property forType(final Property.Type type, final String name, final String title,
                                   final String description, final boolean required,
                                   final String defaultValue, final List<String> values) {
        return forType(type, name, title, description, required, defaultValue, values, null);
    }

    /**
     * Return a property instance for a particular simple type
     */
    public static Property forType(final Property.Type type,
                                   final String name,
                                   final String title,
                                   final String description,
                                   final boolean required,
                                   final String defaultValue,
                                   final List<String> values,
                                   final PropertyValidator validator) {
        switch (type) {
            case Integer:
                return integer(name, title, description, required, defaultValue, validator);
            case Boolean:
                return bool(name, title, description, required, defaultValue);
            case Long:
                return longProp(name, title, description, required, defaultValue, validator);
            case Select:
                return PropertyUtil.select(name, title, description, required, defaultValue, values);
            case FreeSelect:
                return PropertyUtil.freeSelect(name, title, description, required, defaultValue, values, validator);
            default:
                return string(name, title, description, required, defaultValue, validator);
        }
    }

    /**
     * Return a string property
     */
    public static Property string(final String name, final String title, final String description,
                                  final boolean required,
                                  final String defaultValue, final PropertyValidator validator) {
        return new StringProperty(name, title, description, required, defaultValue, validator);
    }

    /**
     * Return a string property
     */
    public static Property string(final String name, final String title, final String description,
                                  final boolean required,
                                  final String defaultValue) {
        return new StringProperty(name, title, description, required, defaultValue, null);
    }

    /**
     * Return a boolean property
     */
    public static Property bool(final String name, final String title, final String description, final boolean required,
                                final String defaultValue) {
        return new BooleanProperty(name, title, description, required, defaultValue);
    }

    /**
     * Return an integer property
     */
    public static Property integer(final String name, final String title, final String description,
                                   final boolean required,
                                   final String defaultValue) {
        return integer(name, title, description, required, defaultValue, null);

    }

    /**
     * Return an integer property with additional validator
     */
    public static Property integer(final String name, final String title, final String description,
                                   final boolean required,
                                   final String defaultValue, final PropertyValidator validator) {

        return new IntegerProperty(name, title, description, required, defaultValue, validator);
    }

    /**
     * Return a long property
     */
    public static Property longProp(final String name, final String title, final String description,
                                    final boolean required, final String defaultValue) {
        return longProp(name, title, description, required, defaultValue, null);
    }

    /**
     * Return a long property
     */
    public static Property longProp(final String name, final String title, final String description,
                                    final boolean required, final String defaultValue, final PropertyValidator validator) {

        return new LongProperty(name, title, description, required, defaultValue, validator);
    }


    /**
     * Create a Select property with a list of values
     */
    public static Property select(final String name, final String title, final String description,
                                  final boolean required, final String defaultValue, final List<String> selectValues) {

        return new SelectProperty(name, title, description, required, defaultValue, selectValues);
    }

    /**
     * Create a Free Select property with a list of values
     */
    public static Property freeSelect(final String name, final String title, final String description,
                                      final boolean required, final String defaultValue,
                                      final List<String> selectValues) {
        return freeSelect(name, title, description, required, defaultValue, selectValues, null);
    }

    /**
     * Create a Free Select property with a list of values
     */
    public static Property freeSelect(final String name, final String title, final String description,
                                      final boolean required, final String defaultValue,
                                      final List<String> selectValues, final PropertyValidator validator) {

        return new FreeSelectProperty(name, title, description, required, defaultValue, selectValues, validator);
    }

    static final class StringProperty extends PropertyBase {

        public StringProperty(final String name, final String title, final String description, final boolean required,
                              final String defaultValue, final PropertyValidator validator) {
            super(name, title, description, required, defaultValue, validator);
        }

        public Type getType() {
            return String;
        }
    }

    static PropertyValidator andValidator(final PropertyValidator first, final PropertyValidator second) {
        if (null == first) {
            return second;
        } else if (null == second) {
            return first;
        }
        return new PropertyValidator() {
            @Override
            public boolean isValid(String value) throws ValidationException {
                return first.isValid(value) && second.isValid(value);
            }
        };
    }

    static final class FreeSelectProperty extends PropertyBase {
        final List<String> selectValues;

        public FreeSelectProperty(final String name,
                                  final String title,
                                  final String description,
                                  final boolean required,
                                  final String defaultValue,
                                  final List<String> selectValues, final PropertyValidator validator) {
            super(name, title, description, required, defaultValue, validator);
            this.selectValues = selectValues;
        }

        public Type getType() {
            return FreeSelect;
        }

        @Override
        public List<String> getSelectValues() {
            return selectValues;
        }
    }

    static final class SelectProperty extends PropertyBase {
        final List<String> selectValues;

        public SelectProperty(final String name, final String title, final String description, final boolean required,
                              final String defaultValue, final List<String> selectValues) {
            super(name, title, description, required, defaultValue, new SelectValidator(selectValues));
            this.selectValues = selectValues;
        }

        public Type getType() {
            return Select;
        }

        @Override
        public List<String> getSelectValues() {
            return selectValues;
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

    static final class BooleanProperty extends PropertyBase {
        public BooleanProperty(final String name, final String title, final String description, final boolean required,
                               final String defaultValue) {
            super(name, title, description, required, defaultValue, booleanValidator);
        }

        public Type getType() {
            return Boolean;
        }
    }

    static final PropertyValidator booleanValidator = new PropertyValidator() {
        public boolean isValid(final String value) throws ValidationException {
            return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
        }
    };

    static final class IntegerProperty extends PropertyBase {
        public IntegerProperty(final String name, final String title, final String description, final boolean required,
                               final String defaultValue, final PropertyValidator validator) {
            super(name, title, description, required, defaultValue, andValidator(integerValidator, validator));
        }

        public Type getType() {
            return Integer;
        }

    }

    static final PropertyValidator integerValidator = new PropertyValidator() {
        public boolean isValid(final String value) throws ValidationException {
            try {
                java.lang.Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new ValidationException("Not a valid integer");
            }
            return true;
        }
    };

    static final class LongProperty extends PropertyBase {
        public LongProperty(final String name, final String title, final String description, final boolean required,
                            final String defaultValue, final PropertyValidator validator) {
            super(name, title, description, required, defaultValue, andValidator(longValidator, validator));
        }

        public Type getType() {
            return Long;
        }

    }

    static final PropertyValidator longValidator = new PropertyValidator() {
        public boolean isValid(final String value) throws ValidationException {
            try {
                java.lang.Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new ValidationException("Not a valid integer");
            }
            return true;
        }
    };

    private static class Generic extends PropertyBase {
        private final Type type;

        public Generic(final String name, final String title, final String description, final boolean required,
                       final String defaultValue,
                       final PropertyValidator validator, final Type type) {
            super(name, title, description, required, defaultValue, validator);
            this.type = type;
        }

        public Type getType() {
            return type;
        }
    }
}
