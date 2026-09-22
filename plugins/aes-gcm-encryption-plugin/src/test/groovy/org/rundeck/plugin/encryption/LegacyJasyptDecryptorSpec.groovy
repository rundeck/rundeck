package org.rundeck.plugin.encryption

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor
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
     * Encrypt data using our Jasypt-compatible encryptor.
     * Produces the same format as Jasypt's StandardPBEByteEncryptor.
     */
    private byte[] jasyptEncrypt(byte[] plaintext, String password, String algorithm, String provider, int iterations) {
        def encryptor = new LegacyJasyptEncryptor(algorithm, provider, iterations)
        return encryptor.encrypt(password, plaintext)
    }

    /**
     * Encrypt data using the actual org.jasypt library's StandardPBEByteEncryptor, to prove
     * LegacyJasyptDecryptor is compatible with real Jasypt output, not just our own
     * reimplementation (RUN-4976 test-coverage gap).
     */
    private byte[] realJasyptEncrypt(byte[] plaintext, String password, String algorithm, String provider, int iterations) {
        def encryptor = new StandardPBEByteEncryptor()
        encryptor.setAlgorithm(algorithm)
        encryptor.setProviderName(provider)
        encryptor.setPassword(password)
        encryptor.setKeyObtentionIterations(iterations)
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

    /**
     * AES-CBC with PKCS#7 does not authenticate ciphertext. A wrong password usually
     * fails padding validation in {@code Cipher.doFinal}, but valid padding can occur
     * by chance (~0.4%), in which case decryption returns garbage bytes instead of
     * throwing. Assert the secure property that always holds: never the original
     * plaintext. (AES-GCM is the authenticated path; see {@link AesEncryptorSpec}.)
     */
    def "decrypt with wrong password does not return original plaintext"() {
        given:
        def plaintext = "secret data".bytes
        def encrypted = jasyptEncrypt(plaintext, "correct-password", "PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000)
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        byte[] result = null
        EncryptionException caught = null
        try {
            result = decryptor.decrypt("wrong-password", encrypted)
        } catch (EncryptionException e) {
            caught = e
        }

        then:
        caught != null || result != plaintext
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

    /**
     * RUN-4976: MeteoSwiss's scm-export.properties bytes are not block-aligned as raw
     * Jasypt binary output, but decrypt correctly once Base64-decoded first. Reproduces
     * the "last block incomplete in decryption" crash-loop with a Base64-wrapped payload.
     */
    def "decrypt recovers Base64-encoded legacy Jasypt payload via fallback"() {
        given:
        def password = "meteoswiss-scm-password"
        def plaintext = "scm.export.enabled=true\nscm.export.branch=main".bytes
        def rawEncrypted = jasyptEncrypt(plaintext, password, "PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000)
        def base64Wrapped = Base64.encoder.encode(rawEncrypted)
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        def result = decryptor.decrypt(password, base64Wrapped)

        then:
        result == plaintext
    }

    /**
     * RUN-4976: after the Base64 fallback is exhausted, genuinely corrupt/undecryptable content
     * (not block-aligned, not Base64, not plaintext-shaped) must still throw -- this is the
     * decryptor-level half of the "SCM config stays loud" guarantee.
     */
    def "decrypt still throws for genuinely corrupt content after Base64 fallback is exhausted"() {
        given: "bytes that are neither valid raw ciphertext nor valid Base64"
        def corrupt = new byte[40]
        Arrays.fill(corrupt, (byte) 0xFF)
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        decryptor.decrypt("some-password", corrupt)

        then:
        thrown(EncryptionException)
    }

    @Unroll
    def "decrypt real Jasypt library (org.jasypt) output for algorithm #algorithm"() {
        given: "data encrypted by the actual Jasypt library, not our reimplementation"
        def password = "real-jasypt-fixture-password"
        def plaintext = "Hello from the real Jasypt library!".bytes
        def encrypted = realJasyptEncrypt(plaintext, password, algorithm, "BC", 1000)

        and:
        def decryptor = new LegacyJasyptDecryptor(algorithm, "BC", 1000)

        when:
        def result = decryptor.decrypt(password, encrypted)

        then:
        result == plaintext

        where:
        algorithm << ["PBEWITHSHA256AND128BITAES-CBC-BC", "PBEWithMD5AndDES"]
    }
}
