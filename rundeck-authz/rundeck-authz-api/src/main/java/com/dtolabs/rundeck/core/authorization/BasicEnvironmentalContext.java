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

package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.EnvironmentalContext;

import java.net.URI;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Basic implementation matches value for equality or as a regular expression if specified.
 * Matches only a single Attribute, and will not match for multiple inputs
 */
public class BasicEnvironmentalContext implements EnvironmentalContext {
    private String key;
    private String value;
    Pattern valuePattern;
    private URI keyUri;

    private BasicEnvironmentalContext(final String key, final String value, final Pattern valuePattern) {
        this.key = key;
        this.value = value;
        this.valuePattern = valuePattern;
        keyUri = URI.create(
                EnvironmentalContext.URI_BASE + key
        );
    }

    /**
     * @param key   key
     * @param value value to check for equality and regular expression if it is a valid regular expression
     *
     * @return context with possible regular expression value match
     */
    public static BasicEnvironmentalContext patternContextFor(String key, String value) {
        if (null == key) {
            throw new IllegalArgumentException("key cannot be null");
        }
        if (null == value) {
            throw new IllegalArgumentException("value cannot be null");
        }
        try {
            return new BasicEnvironmentalContext(key, value, Pattern.compile(value));
        } catch (PatternSyntaxException ignored) {
        }
        return new BasicEnvironmentalContext(key, value, null);
    }

    /**
     * @param key   key
     * @param value value to use for equality match
     *
     * @return context with equality matching
     */
    public static BasicEnvironmentalContext staticContextFor(String key, String value) {
        if (null == key) {
            throw new IllegalArgumentException("key cannot be null");
        }
        if (null == value) {
            throw new IllegalArgumentException("value cannot be null");
        }
        return new BasicEnvironmentalContext(key, value, null);
    }

    @Override
    public boolean matches(final Set<Attribute> environment) {
        if (environment.size() != 1) {
            return false;
        }
        Attribute next = environment.iterator().next();

        if (next.getProperty().equals(getKeyUri())) {
            if (getValue().equals(next.getValue())) {
                return true;
            } else if (null != valuePattern && valuePattern.matcher(next.getValue()).matches()) {
                return true;
            }

        }
        return false;
    }

    @Override
    public String toString() {
        return "{" + getKey() + "='" + getValue() + "'}";
    }

    @Override
    public boolean isValid() {
        return true;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public URI getKeyUri() {
        return keyUri;
    }
}
