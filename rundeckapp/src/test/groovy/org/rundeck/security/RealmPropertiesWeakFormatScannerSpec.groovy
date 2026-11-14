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

import spock.lang.Specification

class RealmPropertiesWeakFormatScannerSpec extends Specification {

    def "finds usernames with MD5, CRYPT, or plaintext entries"() {
        given:
        Properties realmProperties = new Properties()
        realmProperties.setProperty("bcryptUser", "BCRYPT:\$2a\$10\$abcdefghijklmnopqrstuv,user")
        realmProperties.setProperty("md5User", "MD5:5f4dcc3b5aa765d61d8327deb882cf99,user")
        realmProperties.setProperty("cryptUser", "CRYPT:ab12345678901,user")
        realmProperties.setProperty("plaintextUser", "hunter2,user")

        when:
        List<String> weak = RealmPropertiesWeakFormatScanner.findWeakFormatUsernames(realmProperties)

        then:
        weak.size() == 3
        weak.containsAll(["md5User", "cryptUser", "plaintextUser"])
        !weak.contains("bcryptUser")
    }

    def "returns empty list when all entries are BCRYPT encoded"() {
        given:
        Properties realmProperties = new Properties()
        realmProperties.setProperty("user1", "BCRYPT:\$2a\$10\$abcdefghijklmnopqrstuv,user")
        realmProperties.setProperty("user2", "BCRYPT:\$2a\$10\$zyxwvutsrqponmlkjihgfe,admin,user")

        expect:
        RealmPropertiesWeakFormatScanner.findWeakFormatUsernames(realmProperties).isEmpty()
    }

    def "does not flag OBF entries even though they are already rejected at login"() {
        given:
        Properties realmProperties = new Properties()
        realmProperties.setProperty("obfUser", "OBF:1uve1sho1w8h1vgz1vgv1wui1wtw1vfz1vfv1w991shu1uus,user")

        expect: "OBF handling is unchanged by this ticket - only MD5/CRYPT/plaintext are in scope"
        RealmPropertiesWeakFormatScanner.findWeakFormatUsernames(realmProperties).isEmpty()
    }

    def "ignores the role suffix when checking the password prefix"() {
        given:
        Properties realmProperties = new Properties()
        realmProperties.setProperty("user1", "MD5:5f4dcc3b5aa765d61d8327deb882cf99,admin,user,extra")

        expect:
        RealmPropertiesWeakFormatScanner.findWeakFormatUsernames(realmProperties) == ["user1"]
    }

    def "returns empty list for empty or null properties"() {
        expect:
        RealmPropertiesWeakFormatScanner.findWeakFormatUsernames(new Properties()) == []
        RealmPropertiesWeakFormatScanner.findWeakFormatUsernames(null) == []
    }
}
