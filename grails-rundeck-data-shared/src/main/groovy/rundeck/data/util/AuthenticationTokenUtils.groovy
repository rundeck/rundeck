package rundeck.data.util

import org.apache.commons.lang3.RandomStringUtils
import org.rundeck.app.data.model.v1.authtoken.AuthTokenMode
import org.rundeck.app.data.model.v1.authtoken.AuthenticationToken

import java.time.Clock
import java.util.stream.Collectors
import java.util.stream.Stream

class AuthenticationTokenUtils {

    /** Alphabet used for API and webhook auth token generation. */
    static final String SECURE_TOKEN_ALPHABET = 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'

    /** Default length for generated API and webhook auth tokens. */
    static final int SECURE_TOKEN_LENGTH = 32

    /**
     * Generates a cryptographically secure random string for auth tokens.
     *
     * @param length number of characters to generate
     * @param chars allowed character alphabet
     * @return secure random string
     */
    static String generateSecureRandomString(
            int length = SECURE_TOKEN_LENGTH,
            String chars = SECURE_TOKEN_ALPHABET
    ) {
        return RandomStringUtils.secure().next(length, chars)
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
