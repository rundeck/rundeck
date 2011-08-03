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

/**
 * PropertyUtil factory for specific property types
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PropertyUtil {
    /**
     * Return a property instance for a particular type
     */
    public static Property forType(final Property.Type type, final String key, final String name,
                                   final String description, final boolean required,
                                   final String defaultValue, final Property.Validator validator) {
        return new Generic(key, name, description, required, defaultValue, validator, type);
    }

    /**
     * Return a string property
     */
    public static Property string(final String key, final String name, final String description, final boolean required,
                                  final String defaultValue, final Property.Validator validator) {
        return new StringProperty(key, name, description, required, defaultValue, validator);
    }

    /**
     * Return a string property
     */
    public static Property string(final String key, final String name, final String description, final boolean required,
                                  final String defaultValue) {
        return new StringProperty(key, name, description, required, defaultValue, null);
    }

    /**
     * Return a boolean property
     */
    public static Property bool(final String key, final String name, final String description, final boolean required,
                                final String defaultValue) {
        return new BooleanProperty(key, name, description, required, defaultValue);
    }

    /**
     * Return an integer property
     */
    public static Property integer(final String key, final String name, final String description,
                                   final boolean required,
                                   final String defaultValue) {
        return new IntegerProperty(key, name, description, required, defaultValue);
    }

    /**
     * Return a long property
     */
    public static Property longProp(final String key, final String name, final String description,
                                    final boolean required, final String defaultValue) {

        return new LongProperty(key, name, description, required, defaultValue);
    }


    /**
     * Create a Select property with a list of values
     */
    public static Property select(final String key, final String name, final String description,
                                  final boolean required, final String defaultValue, final List<String> selectValues) {

        return new SelectProperty(key, name, description, required, defaultValue, selectValues);
    }

    /**
     * Create a Free Select property with a list of values
     */
    public static Property freeSelect(final String key, final String name, final String description,
                                      final boolean required, final String defaultValue,
                                      final List<String> selectValues) {

        return new FreeSelectProperty(key, name, description, required, defaultValue, selectValues);
    }

    static final class StringProperty extends PropertyBase {

        public StringProperty(final String key, final String name, final String description, final boolean required,
                              final String defaultValue, final Validator validator) {
            super(key, name, description, required, defaultValue, validator);
        }

        public Type getType() {
            return Type.String;
        }
    }

    static final class FreeSelectProperty extends PropertyBase {
        final List<String> selectValues;

        public FreeSelectProperty(final String key, final String name, final String description, final boolean required,
                                  final String defaultValue, final List<String> selectValues) {
            super(key, name, description, required, defaultValue, null);
            this.selectValues = selectValues;
        }

        public Type getType() {
            return Type.FreeSelect;
        }

        @Override
        public List<String> getSelectValues() {
            return selectValues;
        }
    }

    static final class SelectProperty extends PropertyBase {
        final List<String> selectValues;

        public SelectProperty(final String key, final String name, final String description, final boolean required,
                              final String defaultValue, final List<String> selectValues) {
            super(key, name, description, required, defaultValue, new SelectValidator(selectValues));
            this.selectValues = selectValues;
        }

        public Type getType() {
            return Type.Select;
        }

        @Override
        public List<String> getSelectValues() {
            return selectValues;
        }
    }

    static final class SelectValidator implements Property.Validator {

        final List<String> selectValues;

        SelectValidator(final List<String> selectValues) {
            this.selectValues = selectValues;
        }

        public boolean isValid(final String value) throws ValidationException {
            return selectValues.contains(value);
        }
    }

    static final class BooleanProperty extends PropertyBase {
        public BooleanProperty(final String key, final String name, final String description, final boolean required,
                               final String defaultValue) {
            super(key, name, description, required, defaultValue, booleanValidator);
        }

        public Type getType() {
            return Type.Boolean;
        }
    }

    static final Property.Validator booleanValidator = new Property.Validator() {
        public boolean isValid(final String value) throws ValidationException {
            return "true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value);
        }
    };

    static final class IntegerProperty extends PropertyBase {
        public IntegerProperty(final String key, final String name, final String description, final boolean required,
                               final String defaultValue) {
            super(key, name, description, required, defaultValue, integerValidator);
        }

        public Type getType() {
            return Type.Integer;
        }

    }

    static final Property.Validator integerValidator = new Property.Validator() {
        public boolean isValid(final String value) throws ValidationException {
            try {
                Integer.parseInt(value);
            } catch (NumberFormatException e) {
                throw new ValidationException("Not a valid integer");
            }
            return true;
        }
    };

    static final class LongProperty extends PropertyBase {
        public LongProperty(final String key, final String name, final String description, final boolean required,
                            final String defaultValue) {
            super(key, name, description, required, defaultValue, longValidator);
        }

        public Type getType() {
            return Type.Long;
        }

    }

    static final Property.Validator longValidator = new Property.Validator() {
        public boolean isValid(final String value) throws ValidationException {
            try {
                Long.parseLong(value);
            } catch (NumberFormatException e) {
                throw new ValidationException("Not a valid integer");
            }
            return true;
        }
    };

    private static class Generic extends PropertyBase {
        private final Type type;

        public Generic(final String key, final String name, final String description, final boolean required,
                       final String defaultValue,
                       final Validator validator, final Type type) {
            super(key, name, description, required, defaultValue, validator);
            this.type = type;
        }

        public Type getType() {
            return type;
        }
    }
}
