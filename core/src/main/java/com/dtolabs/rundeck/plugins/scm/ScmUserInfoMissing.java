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

package com.dtolabs.rundeck.plugins.scm;

/**
 * Exception representing missing user info during an export action
 */
public class ScmUserInfoMissing extends ScmPluginException {
    private String fieldName;

    public ScmUserInfoMissing(final String fieldName, final String message) {
        super(message);
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    /**
     * Throw a ScmUserInfoMissing
     *
     * @param fieldName name of the missing field
     *
     * @throws ScmUserInfoMissing always
     */
    public static void fieldMissing(String fieldName) throws ScmUserInfoMissing {
        throw new ScmUserInfoMissing(fieldName, "Required user info field was not set: " + fieldName);
    }

    /**
     * Check if an exception represents a missing user info field
     *
     * @param e plugin exception
     *
     * @return true if the exception is a {@link ScmUserInfoMissing}
     */
    public static boolean isFieldMissing(ScmPluginException e) {
        return e instanceof ScmUserInfoMissing;
    }

    /**
     * Returns the missing field name if the exception is a {@link ScmUserInfoMissing}
     *
     * @param e exception
     *
     * @return missing field name, or null
     */
    public static String missingFieldName(ScmPluginException e) {
        if (isFieldMissing(e)) {
            return ((ScmUserInfoMissing) e).getFieldName();
        }
        return null;
    }
}
