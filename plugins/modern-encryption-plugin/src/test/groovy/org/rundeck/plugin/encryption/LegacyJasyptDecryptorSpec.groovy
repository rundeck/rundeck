package org.rundeck.plugin.encryption

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor
import org.jasypt.encryption.pbe.config.SimplePBEConfig
import spock.lang.Specification
import spock.lang.Unroll

import java.security.Security

class LegacyJasyptDecryptorSpec extends Specification {

    def setupSpec() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider())
        }
    }

    /**
     * Encrypt data using the real Jasypt library (test dependency only).
     * This produces authentic Jasypt-format bytes that the decryptor must handle.
     */
    private byte[] jasyptEncrypt(byte[] plaintext, String password, String algorithm, String provider, int iterations) {
        def config = new SimplePBEConfig()
        config.setPassword(password)
        config.setAlgorithm(algorithm)
        if (provider) {
            config.setProviderName(provider)
        }
        config.setKeyObtentionIterations(iterations)

        def encryptor = new StandardPBEByteEncryptor()
        encryptor.setConfig(config)
        return encryptor.encrypt(plaintext)
    }

    @Unroll
    def "decrypt Jasypt data with algorithm #algorithm"() {
        given: "data encrypted by real Jasypt library"
        def password = "test-encryption-password"
        def plaintext = "Hello, Rundeck encryption migration!".bytes
        def encrypted = jasyptEncrypt(plaintext, password, algorithm, provider, 1000)

        and: "our legacy decryptor configured for the same algorithm"
        def decryptor = new LegacyJasyptDecryptor(algorithm, provider, 1000)

        when:
        def result = decryptor.decrypt(password, encrypted)

        then:
        result == plaintext

        where:
        algorithm                          | provider
        "PBEWITHSHA256AND128BITAES-CBC-BC" | "BC"
        "PBEWITHSHA256AND256BITAES-CBC-BC" | "BC"
        "PBEWithMD5AndDES"                 | "BC"
    }

    def "decrypt Rundeck default storage encryption"() {
        given:
        def password = "default.encryption.password"
        def plaintext = "ssh-rsa AAAAB3NzaC1yc2EAAAA... user@host".bytes
        def encrypted = jasyptEncrypt(plaintext, password, "PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000)

        and:
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        def result = decryptor.decrypt(password, encrypted)

        then:
        result == plaintext
    }

    def "decrypt datasource password encryption"() {
        given:
        def password = "my-datasource-secret"
        def plaintext = "jdbc:mysql://db:3306/rundeck?pass=s3cret".bytes
        def encrypted = jasyptEncrypt(plaintext, password, "PBEWITHSHA256AND256BITAES-CBC-BC", "BC", 1000)

        and:
        def decryptor = LegacyJasyptDecryptor.datasourcePassword()

        when:
        def result = decryptor.decrypt(password, encrypted)

        then:
        result == plaintext
    }

    def "decrypt core properties encryption"() {
        given:
        def password = "core-props-secret"
        def plaintext = "my-database-password-value".bytes
        def encrypted = jasyptEncrypt(plaintext, password, "PBEWithMD5AndDES", "BC", 1000)

        and:
        def decryptor = LegacyJasyptDecryptor.coreProperties()

        when:
        def result = decryptor.decrypt(password, encrypted)

        then:
        result == plaintext
    }

    def "salt size matches cipher block size"() {
        expect:
        new LegacyJasyptDecryptor("PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000).saltSizeBytes == 16
        new LegacyJasyptDecryptor("PBEWITHSHA256AND256BITAES-CBC-BC", "BC", 1000).saltSizeBytes == 16
        new LegacyJasyptDecryptor("PBEWithMD5AndDES", "BC", 1000).saltSizeBytes == 8
    }

    def "decrypt with wrong password throws EncryptionException"() {
        given:
        def plaintext = "secret data".bytes
        def encrypted = jasyptEncrypt(plaintext, "correct-password", "PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000)
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        decryptor.decrypt("wrong-password", encrypted)

        then:
        thrown(EncryptionException)
    }

    def "decrypt null message throws EncryptionException"() {
        given:
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        decryptor.decrypt("password", null)

        then:
        thrown(EncryptionException)
    }

    def "decrypt message shorter than salt throws EncryptionException"() {
        given:
        def decryptor = LegacyJasyptDecryptor.defaultStorage()
        def tooShort = new byte[10]

        when:
        decryptor.decrypt("password", tooShort)

        then:
        thrown(EncryptionException)
    }

    def "decrypt large data (1MB)"() {
        given:
        def password = "large-data-password"
        def plaintext = new byte[1024 * 1024]
        new Random(42).nextBytes(plaintext)
        def encrypted = jasyptEncrypt(plaintext, password, "PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000)

        and:
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        def result = decryptor.decrypt(password, encrypted)

        then:
        result == plaintext
    }

    def "multiple encrypt-decrypt cycles produce consistent results"() {
        given:
        def password = "consistency-test"
        def plaintext = "same data encrypted multiple times".bytes
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when: "encrypt the same data 5 times (each with different random salt)"
        def results = (1..5).collect {
            def encrypted = jasyptEncrypt(plaintext, password, "PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000)
            decryptor.decrypt(password, encrypted)
        }

        then: "all decrypt to the same plaintext"
        results.every { it == plaintext }
    }

    def "char array password overload works"() {
        given:
        def password = "char-array-password".toCharArray()
        def plaintext = "test with char array".bytes
        def encrypted = jasyptEncrypt(plaintext, new String(password), "PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000)
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        def result = decryptor.decrypt(password, encrypted)

        then:
        result == plaintext
    }
}
