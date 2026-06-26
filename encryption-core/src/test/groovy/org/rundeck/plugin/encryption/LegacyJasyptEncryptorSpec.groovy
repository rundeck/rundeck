package org.rundeck.plugin.encryption

import org.bouncycastle.jce.provider.BouncyCastleProvider
import spock.lang.Specification
import spock.lang.Unroll

import java.security.Security

class LegacyJasyptEncryptorSpec extends Specification {

    def setupSpec() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider())
        }
    }

    @Unroll
    def "encrypt-decrypt round-trip with algorithm #algorithm"() {
        given:
        def password = "round-trip-test-password"
        def plaintext = "Hello, Rundeck encryption!".bytes
        def encryptor = new LegacyJasyptEncryptor(algorithm, provider, 1000)
        def decryptor = new LegacyJasyptDecryptor(algorithm, provider, 1000)

        when:
        def encrypted = encryptor.encrypt(password, plaintext)
        def decrypted = decryptor.decrypt(password, encrypted)

        then:
        decrypted == plaintext
        encrypted != plaintext

        where:
        algorithm                          | provider
        "PBEWITHSHA256AND128BITAES-CBC-BC" | "BC"
        "PBEWITHSHA256AND256BITAES-CBC-BC" | "BC"
        "PBEWithMD5AndDES"                 | "BC"
    }

    @Unroll
    def "factory method #factoryMethod produces correct salt size"() {
        when:
        def encryptor = LegacyJasyptEncryptor."$factoryMethod"()

        then:
        encryptor.saltSizeBytes == expectedSaltSize

        where:
        factoryMethod       | expectedSaltSize
        "defaultStorage"    | 16
        "datasourcePassword"| 16
        "coreProperties"    | 8
    }

    def "each encryption produces different output (random salt)"() {
        given:
        def password = "salt-randomness-test"
        def plaintext = "same input every time".bytes
        def encryptor = LegacyJasyptEncryptor.defaultStorage()

        when:
        def encrypted1 = encryptor.encrypt(password, plaintext)
        def encrypted2 = encryptor.encrypt(password, plaintext)

        then:
        encrypted1 != encrypted2
    }

    def "encrypt null plaintext throws EncryptionException"() {
        given:
        def encryptor = LegacyJasyptEncryptor.defaultStorage()

        when:
        encryptor.encrypt("password", (byte[]) null)

        then:
        thrown(EncryptionException)
    }

    def "encrypt empty plaintext succeeds"() {
        given:
        def password = "empty-test"
        def encryptor = LegacyJasyptEncryptor.defaultStorage()
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        def encrypted = encryptor.encrypt(password, new byte[0])
        def decrypted = decryptor.decrypt(password, encrypted)

        then:
        decrypted == new byte[0]
    }

    def "encrypt large data (1MB)"() {
        given:
        def password = "large-data-test"
        def plaintext = new byte[1024 * 1024]
        new Random(42).nextBytes(plaintext)
        def encryptor = LegacyJasyptEncryptor.defaultStorage()
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        def encrypted = encryptor.encrypt(password, plaintext)
        def decrypted = decryptor.decrypt(password, encrypted)

        then:
        decrypted == plaintext
    }

    def "char array password overload works"() {
        given:
        def password = "char-array-password".toCharArray()
        def plaintext = "test with char array".bytes
        def encryptor = LegacyJasyptEncryptor.defaultStorage()
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        def encrypted = encryptor.encrypt(password, plaintext)
        def decrypted = decryptor.decrypt(new String(password), encrypted)

        then:
        decrypted == plaintext
    }

}
