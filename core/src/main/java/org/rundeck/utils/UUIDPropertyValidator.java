/*
 * Copyright 2019 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.utils;

import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator;
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Property validator for UUID input, which expects exact format '01234567-89ab-cdef-0123-456789abcdef'
 */
public class UUIDPropertyValidator
    implements PropertyValidator
{
    private static final Pattern UUID_PAT = Pattern.compile(
        "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$"
    );

    @Override
    public boolean isValid(final String value) throws ValidationException {
        return validate(value);
    }

    /**
     * Return true if the value is valid
     *
     * @param value input value
     * @return true if valid, false if not
     */
    public static boolean isValidUUID(final String value) {
        try {
            return validate(value, false);
        } catch (ValidationException ignored) {
            return false;
        }
    }

    /**
     * Validate the input
     *
     * @param value input value
     * @return true if valid
     * @throws ValidationException if invalid
     */
    public static boolean validate(final String value) throws ValidationException {
        return validate(value, true);
    }

    private static boolean validate(final String value, boolean report) throws ValidationException {
        if (null == value || value.length() != 36) {
            if (!report) {
                return false;
            }
            throw new ValidationException(String.format("Expected 36 characters, saw input: '%s'", value));

        }
        if (!UUID_PAT.matcher(value).matches()) {
            if (!report) {
                return false;
            }
            throw new ValidationException(String.format("Expected valid UUID, saw input: '%s'", value));
        }
        try {
            UUID ignored = UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            if (!report) {
                return false;
            }
            throw new ValidationException(e.getMessage(), e);
        }
        return true;
    }
}
