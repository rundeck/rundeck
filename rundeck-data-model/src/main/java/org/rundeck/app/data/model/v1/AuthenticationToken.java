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
package org.rundeck.app.data.model.v1;

import org.rundeck.app.data.utils.Utils;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rundeck.app.data.model.v1.AuthTokenMode.*;

public interface AuthenticationToken {
    String getToken();
    Set<String> getAuthRolesSet();
    String getUuid();
    String getCreator();
    String getOwnerName();
    AuthTokenType getType();
    String getPrintableToken();
    Date getExpiration();
    String getName();
    String getClearToken();
    AuthTokenMode getTokenMode();

    static enum AuthTokenType {
        USER,
        WEBHOOK,
        RUNNER
    }

    static boolean tokenIsExpired(AuthenticationToken token) {
        return token.getExpiration() != null && (
                token.getExpiration().getTime() < Date
                        .from(Clock.systemUTC().instant())
                        .getTime()
        );
    }

    static String generateAuthRoles(Collection<String> roles) {
        return roles
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !"".equals(s))
                .collect(Collectors.joining(","));
    }

    static Set<String> parseAuthRoles(String authRoles){
        if(authRoles == null || authRoles.trim().isEmpty()) {
            return Collections.emptySet();
        }

        return Stream.of(authRoles.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    /**
     * Encodes a clear token value according to the tokenMode supplied.
     */
     static String encodeTokenValue(String clearValue, AuthTokenMode mode){
        if(clearValue == null || clearValue.trim().isEmpty())
            throw new IllegalArgumentException("Illegal token value supplied: " + clearValue);

        switch (mode) {
            case SECURED:
                return Utils.encodeAsSHA256(clearValue);
            case LEGACY:
                return clearValue;
            default:
                return clearValue;
        }
    }

    /**
     * @return Printable truncated token value
     */
    static String printable(String authtoken) {
        return (authtoken.length() > 5 ? authtoken.substring(0, 5) : "") + "****";
    }
}
