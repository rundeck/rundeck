package rundeck.data.util

import org.rundeck.app.data.model.v1.AuthTokenMode
import org.rundeck.app.data.model.v1.AuthenticationToken

import java.time.Clock
import java.util.stream.Collectors
import java.util.stream.Stream

class AuthenticationTokenUtils {

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
     * @param clearValue
     * @param mode
     */
    static String encodeTokenValue(String clearValue, AuthTokenMode mode){
        if(clearValue == null || clearValue.trim().isEmpty())
            throw new IllegalArgumentException("Illegal token value supplied: " + clearValue);

        switch (mode) {
            case AuthTokenMode.SECURED:
                return clearValue.sha256();
            case AuthTokenMode.LEGACY:
                return clearValue;
            default:
                return clearValue;
        }
    }

    /**
     * @param authtoken
     * @return Printable truncated token value
     */
    static String printable(String authtoken) {
        return (authtoken.length() > 5 ? authtoken.substring(0, 5) : "") + "****";
    }
}
