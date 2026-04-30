package org.rundeck.plugin.encryption

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.rundeck.storage.api.HasInputStream
import org.rundeck.storage.api.Path
import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder
import spock.lang.Specification

import java.security.Security

class ModernEncryptionConverterPluginSpec extends Specification {

    static final String TEST_PASSWORD = "test-encryption-password-42"

    def setupSpec() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider())
        }
    }

    private ModernEncryptionConverterPlugin createPlugin(String password = TEST_PASSWORD) {
        def plugin = new ModernEncryptionConverterPlugin()
        plugin.password = password
        plugin.legacyEncryptorType = "custom"
        return plugin
    }

    private ResourceMetaBuilder metaWith(Map<String, String> entries) {
        def meta = new ResourceMetaBuilder()
        entries.each { k, v -> meta.getResourceMeta().put(k, v) }
        return meta
    }

    private HasInputStream mockHasInputStream(byte[] data) {
        return new HasInputStream() {
            @Override
            InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(data)
            }

            @Override
            long writeContent(OutputStream outputStream) throws IOException {
                def is = getInputStream()
                def buf = new byte[1024]
                long total = 0
                int read
                while ((read = is.read(buf)) != -1) {
                    outputStream.write(buf, 0, read)
                    total += read
                }
                return total
            }
        }
    }

    private byte[] readAllBytes(HasInputStream his) {
        def baos = new ByteArrayOutputStream()
        his.writeContent(baos)
        return baos.toByteArray()
    }

    private byte[] jasyptEncrypt(byte[] plaintext, String password, String algorithm, String provider) {
        def encryptor = new LegacyJasyptEncryptor(algorithm, provider, 1000)
        return encryptor.encrypt(password, plaintext)
    }

    // --- createResource tests ---

    def "createResource encrypts data and sets modern metadata"() {
        given:
        def plugin = createPlugin()
        def plaintext = "secret key data".bytes
        def input = mockHasInputStream(plaintext)
        def meta = metaWith([:])
        def path = Mock(Path)

        when:
        def result = plugin.createResource(path, meta, input)

        then:
        result != null
        meta.getResourceMeta()["modern-encryption:encrypted"] == "true"

        and: "encrypted data is different from plaintext"
        def encrypted = readAllBytes(result)
        encrypted != plaintext
        encrypted[0] == ModernEncryptor.FORMAT_VERSION
    }

    def "createResource output can be decrypted by readResource"() {
        given:
        def plugin = createPlugin()
        def plaintext = "round trip test data".bytes
        def path = Mock(Path)

        and: "encrypt via createResource"
        def createMeta = metaWith([:])
        def encrypted = plugin.createResource(path, createMeta, mockHasInputStream(plaintext))
        def encryptedBytes = readAllBytes(encrypted)

        and: "read uses the same metadata that createResource set"
        def readMeta = metaWith(createMeta.getResourceMeta())

        when:
        def decrypted = plugin.readResource(path, readMeta, mockHasInputStream(encryptedBytes))

        then:
        readAllBytes(decrypted) == plaintext
    }

    // --- updateResource tests ---

    def "updateResource encrypts data, sets modern metadata and removes jasypt metadata"() {
        given:
        def plugin = createPlugin()
        def plaintext = "updated data".bytes
        def meta = metaWith(["jasypt-encryption:encrypted": "true"])
        def path = Mock(Path)

        when:
        def result = plugin.updateResource(path, meta, mockHasInputStream(plaintext))

        then:
        result != null
        meta.getResourceMeta()["modern-encryption:encrypted"] == "true"
        meta.getResourceMeta()["jasypt-encryption:encrypted"] == "false"

        and: "output is AES-256-GCM format"
        def encrypted = readAllBytes(result)
        encrypted[0] == ModernEncryptor.FORMAT_VERSION
    }

    // --- readResource tests ---

    def "readResource returns null for unencrypted data"() {
        given:
        def plugin = createPlugin()
        def meta = metaWith([:])
        def path = Mock(Path)

        when:
        def result = plugin.readResource(path, meta, mockHasInputStream("plain data".bytes))

        then:
        result == null
    }

    def "readResource decrypts modern-encrypted data"() {
        given:
        def plugin = createPlugin()
        def plaintext = "modern encrypted secret".bytes
        def path = Mock(Path)

        and: "encrypt first"
        def encryptor = new ModernEncryptor()
        def encrypted = encryptor.encrypt(TEST_PASSWORD, plaintext)

        and:
        def meta = metaWith(["modern-encryption:encrypted": "true"])

        when:
        def result = plugin.readResource(path, meta, mockHasInputStream(encrypted))

        then:
        readAllBytes(result) == plaintext
    }

    def "readResource decrypts jasypt-encrypted data (legacy fallback)"() {
        given:
        def plugin = createPlugin()
        def plaintext = "jasypt encrypted secret".bytes
        def path = Mock(Path)

        and: "encrypt with real Jasypt using Rundeck default algorithm"
        def jasyptEncrypted = jasyptEncrypt(plaintext, TEST_PASSWORD,
                "PBEWITHSHA256AND128BITAES-CBC-BC", "BC")

        and:
        def meta = metaWith(["jasypt-encryption:encrypted": "true"])

        when:
        def result = plugin.readResource(path, meta, mockHasInputStream(jasyptEncrypted))

        then:
        readAllBytes(result) == plaintext
    }

    def "readResource decrypts jasypt basic (PBEWithMD5AndDES) data"() {
        given:
        def plugin = createPlugin()
        plugin.legacyEncryptorType = "basic"
        def plaintext = "basic jasypt secret".bytes
        def path = Mock(Path)

        and:
        def jasyptEncrypted = jasyptEncrypt(plaintext, TEST_PASSWORD,
                "PBEWithMD5AndDES", "BC")

        and:
        def meta = metaWith(["jasypt-encryption:encrypted": "true"])

        when:
        def result = plugin.readResource(path, meta, mockHasInputStream(jasyptEncrypted))

        then:
        readAllBytes(result) == plaintext
    }

    // --- Full lifecycle: jasypt read → modern update → modern read ---

    def "full migration lifecycle: read jasypt → update to modern → read modern"() {
        given:
        def plugin = createPlugin()
        def plaintext = "lifecycle test data".bytes
        def path = Mock(Path)

        and: "original data encrypted by Jasypt"
        def jasyptEncrypted = jasyptEncrypt(plaintext, TEST_PASSWORD,
                "PBEWITHSHA256AND128BITAES-CBC-BC", "BC")

        when: "read the jasypt-encrypted data"
        def readMeta = metaWith(["jasypt-encryption:encrypted": "true"])
        def decrypted = plugin.readResource(path, readMeta, mockHasInputStream(jasyptEncrypted))

        then: "data is correctly decrypted"
        def decryptedBytes = readAllBytes(decrypted)
        decryptedBytes == plaintext

        when: "update re-encrypts with modern encryption"
        def updateMeta = metaWith(["jasypt-encryption:encrypted": "true"])
        def modernEncrypted = plugin.updateResource(path, updateMeta, mockHasInputStream(plaintext))
        def modernEncryptedBytes = readAllBytes(modernEncrypted)

        then: "metadata migrated"
        updateMeta.getResourceMeta()["modern-encryption:encrypted"] == "true"
        updateMeta.getResourceMeta()["jasypt-encryption:encrypted"] == "false"

        when: "read the modern-encrypted data"
        def readMeta2 = metaWith(updateMeta.getResourceMeta())
        def decrypted2 = plugin.readResource(path, readMeta2, mockHasInputStream(modernEncryptedBytes))

        then: "original plaintext recovered"
        readAllBytes(decrypted2) == plaintext
    }

    // --- Password resolution tests ---

    def "password from system property"() {
        given:
        def propName = "test.modern.encryption.password." + System.nanoTime()
        System.setProperty(propName, TEST_PASSWORD)

        and:
        def plugin = new ModernEncryptionConverterPlugin()
        plugin.passwordSysPropName = propName

        when:
        def resolved = plugin.getResolvedPassword()

        then:
        resolved == TEST_PASSWORD.toCharArray()

        cleanup:
        System.clearProperty(propName)
    }

    def "missing password throws IllegalStateException"() {
        given:
        def plugin = new ModernEncryptionConverterPlugin()

        when:
        plugin.getResolvedPassword()

        then:
        thrown(IllegalStateException)
    }

    def "direct password takes precedence over system property"() {
        given:
        def propName = "test.modern.enc.pw." + System.nanoTime()
        System.setProperty(propName, "sys-prop-password")

        and:
        def plugin = new ModernEncryptionConverterPlugin()
        plugin.password = "direct-password"
        plugin.passwordSysPropName = propName

        when:
        def resolved = plugin.getResolvedPassword()

        then:
        resolved == "direct-password".toCharArray()

        cleanup:
        System.clearProperty(propName)
    }

    // --- Large data test ---

    def "encrypt and decrypt large data (1MB)"() {
        given:
        def plugin = createPlugin()
        def plaintext = new byte[1024 * 1024]
        new Random(123).nextBytes(plaintext)
        def path = Mock(Path)

        and:
        def createMeta = metaWith([:])
        def encrypted = plugin.createResource(path, createMeta, mockHasInputStream(plaintext))
        def encryptedBytes = readAllBytes(encrypted)

        and:
        def readMeta = metaWith(createMeta.getResourceMeta())

        when:
        def decrypted = plugin.readResource(path, readMeta, mockHasInputStream(encryptedBytes))

        then:
        readAllBytes(decrypted) == plaintext
    }

    // --- Empty data test ---

    def "encrypt and decrypt empty data"() {
        given:
        def plugin = createPlugin()
        def plaintext = new byte[0]
        def path = Mock(Path)

        and:
        def createMeta = metaWith([:])
        def encrypted = plugin.createResource(path, createMeta, mockHasInputStream(plaintext))
        def encryptedBytes = readAllBytes(encrypted)

        and:
        def readMeta = metaWith(createMeta.getResourceMeta())

        when:
        def decrypted = plugin.readResource(path, readMeta, mockHasInputStream(encryptedBytes))

        then:
        readAllBytes(decrypted) == plaintext
    }

    // --- Legacy algorithm override ---

    def "custom legacy algorithm override (256-bit AES)"() {
        given:
        def plugin = createPlugin()
        plugin.legacyEncryptorType = "custom"
        plugin.legacyAlgorithm = "PBEWITHSHA256AND256BITAES-CBC-BC"
        plugin.legacyProvider = "BC"
        def plaintext = "256-bit key test".bytes
        def path = Mock(Path)

        and:
        def jasyptEncrypted = jasyptEncrypt(plaintext, TEST_PASSWORD,
                "PBEWITHSHA256AND256BITAES-CBC-BC", "BC")

        and:
        def meta = metaWith(["jasypt-encryption:encrypted": "true"])

        when:
        def result = plugin.readResource(path, meta, mockHasInputStream(jasyptEncrypted))

        then:
        readAllBytes(result) == plaintext
    }

    // --- Wrong password test ---

    def "readResource with wrong password throws exception"() {
        given:
        def plugin = createPlugin("wrong-password")
        def encryptor = new ModernEncryptor()
        def encrypted = encryptor.encrypt(TEST_PASSWORD, "secret".bytes)
        def path = Mock(Path)

        def meta = metaWith(["modern-encryption:encrypted": "true"])

        when:
        def result = plugin.readResource(path, meta, mockHasInputStream(encrypted))
        readAllBytes(result)

        then:
        thrown(Exception)
    }
}
