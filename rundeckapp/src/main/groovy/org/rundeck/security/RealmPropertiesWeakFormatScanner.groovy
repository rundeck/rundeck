/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.security

import groovy.transform.CompileStatic

/**
 * Scans a {@code realm.properties}-formatted {@link Properties} object for accounts
 * stored in a weak or unrecognized password format (anything other than {@code BCRYPT:}
 * or the already-rejected {@code OBF:}), so callers can warn admins to migrate them.
 */
@CompileStatic
class RealmPropertiesWeakFormatScanner {

    /**
     * Finds usernames whose realm.properties entry is not {@code BCRYPT:}- or
     * {@code OBF:}-prefixed. Covers {@code MD5:}, {@code CRYPT:}, and bare plaintext
     * entries (no recognized prefix).
     * @param realmProperties realm.properties entries, each value in
     * {@code password[,role1,role2,...]} format
     * @return usernames with a weak or unrecognized password format, in no particular order
     */
    static List<String> findWeakFormatUsernames(Properties realmProperties) {
        List<String> weakUsernames = []
        realmProperties?.each { Object username, Object value ->
            String encodedPassword = value?.toString()?.split(',', 2)?.getAt(0)
            if (encodedPassword && !isStrongFormat(encodedPassword)) {
                weakUsernames << username.toString()
            }
        }
        return weakUsernames
    }

    private static boolean isStrongFormat(String encodedPassword) {
        encodedPassword.startsWith("BCRYPT:") || encodedPassword.startsWith("OBF:")
    }
}
