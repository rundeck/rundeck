package org.rundeck.plugin.encryption

import spock.lang.Specification

import java.nio.ByteBuffer
import java.util.Arrays

class ModernEncryptorSpec extends Specification {

    def encryptor = new ModernEncryptor()

    def "encrypt and decrypt round-trip"() {
        given:
        def password = "test-password-123"
        def plaintext = "Hello, AES-256-GCM!".bytes

        when:
        def encrypted = encryptor.encrypt(password, plaintext)
        def decrypted = encryptor.decrypt(password, encrypted)

        then:
        decrypted == plaintext
    }

    def "encrypted output starts with version byte"() {
        given:
        def encrypted = encryptor.encrypt("password", "data".bytes)

        expect:
        encrypted[0] == ModernEncryptor.FORMAT_VERSION
    }

    def "encrypted output has correct structure"() {
        given:
        def plaintext = "test data".bytes
        def encrypted = encryptor.encrypt("password", plaintext)

        expect: "version(1) + salt(16) + iv(12) + ciphertext(>= plaintext) + tag(16)"
        encrypted.length >= 1 + 16 + 12 + plaintext.length + 16
    }

    def "each encryption produces different output (random salt and IV)"() {
        given:
        def password = "same-password"
        def plaintext = "same plaintext".bytes

        when:
        def encrypted1 = encryptor.encrypt(password, plaintext)
        def encrypted2 = encryptor.encrypt(password, plaintext)

        then: "both decrypt to same plaintext"
        encryptor.decrypt(password, encrypted1) == plaintext
        encryptor.decrypt(password, encrypted2) == plaintext

        and: "but the encrypted bytes differ"
        encrypted1 != encrypted2
    }

    def "wrong password fails with EncryptionException"() {
        given:
        def encrypted = encryptor.encrypt("correct-password", "secret".bytes)

        when:
        encryptor.decrypt("wrong-password", encrypted)

        then:
        def e = thrown(EncryptionException)
        e.message.contains("authentication tag mismatch")
    }

    def "tampered ciphertext fails with EncryptionException"() {
        given:
        def encrypted = encryptor.encrypt("password", "secret data".bytes)

        and: "flip a byte in the ciphertext area"
        def tampered = Arrays.copyOf(encrypted, encrypted.length)
        tampered[encrypted.length - 5] = (byte) (tampered[encrypted.length - 5] ^ 0xFF)

        when:
        encryptor.decrypt("password", tampered)

        then:
        def e = thrown(EncryptionException)
        e.message.contains("authentication tag mismatch")
    }

    def "tampered salt fails decryption"() {
        given:
        def encrypted = encryptor.encrypt("password", "secret data".bytes)

        and: "modify the salt (byte index 1-16)"
        def tampered = Arrays.copyOf(encrypted, encrypted.length)
        tampered[5] = (byte) (tampered[5] ^ 0xFF)

        when:
        encryptor.decrypt("password", tampered)

        then:
        thrown(EncryptionException)
    }

    def "tampered IV fails decryption"() {
        given:
        def encrypted = encryptor.encrypt("password", "secret data".bytes)

        and: "modify the IV (byte index 17-28)"
        def tampered = Arrays.copyOf(encrypted, encrypted.length)
        tampered[20] = (byte) (tampered[20] ^ 0xFF)

        when:
        encryptor.decrypt("password", tampered)

        then:
        thrown(EncryptionException)
    }

    def "null plaintext throws EncryptionException"() {
        when:
        encryptor.encrypt("password", null)

        then:
        thrown(EncryptionException)
    }

    def "null encrypted data throws EncryptionException"() {
        when:
        encryptor.decrypt("password", null)

        then:
        thrown(EncryptionException)
    }

    def "too-short encrypted data throws EncryptionException"() {
        when:
        encryptor.decrypt("password", new byte[10])

        then:
        thrown(EncryptionException)
    }

    def "wrong version byte throws EncryptionException"() {
        given:
        def encrypted = encryptor.encrypt("password", "data".bytes)

        and:
        def badVersion = Arrays.copyOf(encrypted, encrypted.length)
        badVersion[0] = (byte) 0x99

        when:
        encryptor.decrypt("password", badVersion)

        then:
        def e = thrown(EncryptionException)
        e.message.contains("Unsupported encryption format version")
    }

    def "empty plaintext encrypts and decrypts"() {
        given:
        def plaintext = new byte[0]

        when:
        def encrypted = encryptor.encrypt("password", plaintext)
        def decrypted = encryptor.decrypt("password", encrypted)

        then:
        decrypted == plaintext
    }

    def "large data (1MB) encrypts and decrypts"() {
        given:
        def plaintext = new byte[1024 * 1024]
        new Random(42).nextBytes(plaintext)

        when:
        def encrypted = encryptor.encrypt("password", plaintext)
        def decrypted = encryptor.decrypt("password", encrypted)

        then:
        decrypted == plaintext
    }

    def "char array password works"() {
        given:
        def password = "char-array-test".toCharArray()
        def plaintext = "test data".bytes

        when:
        def encrypted = encryptor.encrypt(password, plaintext)
        def decrypted = encryptor.decrypt(password, encrypted)

        then:
        decrypted == plaintext
    }

    def "isModernFormat detects modern encrypted data"() {
        given:
        def encrypted = encryptor.encrypt("password", "data".bytes)

        expect:
        ModernEncryptor.isModernFormat(encrypted)
    }

    def "isModernFormat rejects non-modern data"() {
        expect:
        !ModernEncryptor.isModernFormat(null)
        !ModernEncryptor.isModernFormat(new byte[0])
        !ModernEncryptor.isModernFormat(new byte[5])
        !ModernEncryptor.isModernFormat([0x00, 0x01, 0x02] as byte[])
    }

    def "isModernFormat rejects data with wrong version"() {
        given:
        def data = new byte[50]
        data[0] = (byte) 0x02

        expect:
        !ModernEncryptor.isModernFormat(data)
    }

    def "multiple encryptors produce interoperable output"() {
        given:
        def encryptor1 = new ModernEncryptor()
        def encryptor2 = new ModernEncryptor()
        def password = "shared-password"
        def plaintext = "interop test".bytes

        when:
        def encrypted = encryptor1.encrypt(password, plaintext)
        def decrypted = encryptor2.decrypt(password, encrypted)

        then:
        decrypted == plaintext
    }
}
