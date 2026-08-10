package rundeck.data.util

import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Specification

class AuthenticationTokenUtilsSpec extends Specification {

    def "generateSecureRandomString delegates to RandomStringUtils.secure()"() {
        given:
        boolean usedSecure = false
        RandomStringUtils.metaClass.static.secure = { ->
            usedSecure = true
            return [
                next: { int length, String chars ->
                    assert length == AuthenticationTokenUtils.SECURE_TOKEN_LENGTH
                    assert chars == AuthenticationTokenUtils.SECURE_TOKEN_ALPHABET
                    return 'a' * length
                }
            ]
        }

        when:
        def result = AuthenticationTokenUtils.generateSecureRandomString()

        then:
        usedSecure
        result == 'a' * AuthenticationTokenUtils.SECURE_TOKEN_LENGTH

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(RandomStringUtils)
    }

    def "generateSecureRandomString respects custom length and alphabet"() {
        given:
        boolean usedSecure = false
        RandomStringUtils.metaClass.static.secure = { ->
            usedSecure = true
            return [
                next: { int length, String chars ->
                    assert length == 16
                    assert chars == 'ABC'
                    return 'A' * length
                }
            ]
        }

        when:
        def result = AuthenticationTokenUtils.generateSecureRandomString(16, 'ABC')

        then:
        usedSecure
        result == 'A' * 16

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(RandomStringUtils)
    }

    def "generateSecureRandomString does not call insecure RandomStringUtils.random()"() {
        given:
        RandomStringUtils.metaClass.static.random = { Object... args ->
            throw new AssertionError('insecure random() must not be used for auth tokens')
        }
        RandomStringUtils.metaClass.static.secure = { ->
            return [
                next: { int length, String chars -> 'z' * length }
            ]
        }

        when:
        def result = AuthenticationTokenUtils.generateSecureRandomString()

        then:
        result == 'z' * AuthenticationTokenUtils.SECURE_TOKEN_LENGTH

        cleanup:
        GroovySystem.metaClassRegistry.removeMetaClass(RandomStringUtils)
    }

    def "generateSecureRandomString produces unique values across repeated calls"() {
        when:
        def tokens = (1..50).collect {
            AuthenticationTokenUtils.generateSecureRandomString()
        }

        then:
        tokens.every { it.length() == AuthenticationTokenUtils.SECURE_TOKEN_LENGTH }
        tokens.every { it ==~ /[a-zA-Z0-9]{32}/ }
        tokens.toSet().size() == tokens.size()
    }
}
