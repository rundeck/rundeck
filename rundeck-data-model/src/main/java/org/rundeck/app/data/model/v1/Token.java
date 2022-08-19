package org.rundeck.app.data.model.v1;

import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;

public interface Token {
    String getToken();

    Set<String> getAuthRolesSet();

    String getUuid();

    String getCreator();

    String getOwnerName();

    AuthTokenType getType();

    String getPrintableToken();

    Date getExpiration();

    String getName();

    static enum AuthTokenType {
        USER,
        WEBHOOK,
        RUNNER
    }

    static boolean tokenIsExpired(Token token) {
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

}
