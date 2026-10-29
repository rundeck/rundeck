package org.rundeck.plugin.encryption

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.jasypt.encryption.pbe.StandardPBEByteEncryptor
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
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
     * reimplementation.
     */
    private byte[] realJasyptEncrypt(byte[] plaintext, String password, String algorithm, String provider, int iterations) {
        def encryptor = new StandardPBEByteEncryptor()
        encryptor.setAlgorithm(algorithm)
        encryptor.setProviderName(provider)
        encryptor.setPassword(password)
        encryptor.setKeyObtentionIterations(iterations)
        return encryptor.encrypt(plaintext)
    }

    /**
     * Encrypt a String using the actual org.jasypt library's StandardPBEStringEncryptor, which
     * Base64-encodes its binary output as a String. Used to reproduce, with the real library
     * (not an assumption about it), the exact byte shape that broke the raw-binary-only decryptor.
     */
    private String realJasyptStringEncrypt(String plaintext, String password, String algorithm, String provider, int iterations) {
        def encryptor = new StandardPBEStringEncryptor()
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
     * Reproduces a real crash-loop signature ("last block incomplete in decryption") seen when
     * stored bytes turn out not to be block-aligned raw Jasypt binary output. Uses the actual
     * org.jasypt library's StandardPBEStringEncryptor -- which Base64-encodes its binary output
     * as a String -- rather than manually Base64-wrapping our own reimplementation's output, so
     * this proves compatibility with real library behavior, not just an assumption about it.
     */
    def "decrypt recovers Base64-encoded legacy Jasypt payload via fallback"() {
        given:
        def password = "scm-config-password"
        def plaintext = "scm.export.enabled=true\nscm.export.branch=main"
        def base64String = realJasyptStringEncrypt(plaintext, password, "PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000)
        def storedBytes = base64String.getBytes("UTF-8")
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        def result = decryptor.decrypt(password, storedBytes)

        then:
        new String(result, "UTF-8") == plaintext
    }

    /**
     * Regression for a subtle correctness bug in the fallback ordering: a Base64-encoded payload
     * can be coincidentally block-aligned when its raw ASCII bytes are misread as [salt][ciphertext]
     * too (e.g. a 48-byte payload Base64-encodes to exactly 64 bytes, and 64-16=48 is itself a
     * multiple of 16 -- the same shape a genuine raw ciphertext would have). Before decrypt()
     * preferred the Base64-decoded interpretation whenever it is plausibly shaped, the coincidentally
     * block-aligned raw (wrong) interpretation was tried first and had a small but real chance
     * (~1/256) of "succeeding" with garbage instead of ever reaching the correct decode. The 17-byte
     * plaintext below is chosen specifically to produce this ambiguous shape deterministically
     * (independent of the random salt), so this test is not flaky: decrypt() must now always return
     * the correct plaintext.
     */
    def "decrypt returns correct plaintext when the Base64-decoded shape is ambiguous with a coincidentally block-aligned raw interpretation"() {
        given: "a 17-byte plaintext: [16-byte salt][32-byte padded ciphertext] = 48 raw bytes, which Base64-encodes to exactly 64 ASCII bytes"
        def password = "ambiguous-shape-password"
        def plaintext = "17-byte-value!!!!"
        assert plaintext.bytes.length == 17
        def base64String = realJasyptStringEncrypt(plaintext, password, "PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000)
        def storedBytes = base64String.getBytes("UTF-8")
        assert storedBytes.length == 64

        and:
        def decryptor = LegacyJasyptDecryptor.defaultStorage()

        when:
        def result = decryptor.decrypt(password, storedBytes)

        then: "the correctly-decoded plaintext is returned, not a coincidental garbage 'success' from misreading the raw ASCII bytes as ciphertext"
        new String(result, "UTF-8") == plaintext
    }

    /**
     * After the Base64 fallback is exhausted, genuinely corrupt/undecryptable content (not
     * block-aligned, not Base64, not plaintext-shaped) must still throw -- this is the
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
