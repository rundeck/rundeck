package org.rundeck.plugin.encryption

import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.rundeck.storage.api.HasInputStream
import org.rundeck.storage.api.Path
import spock.lang.Specification

import java.security.Security

class JasyptEncryptionAliasPluginSpec extends Specification {

    static final String TEST_PASSWORD = "test-encryption-password-42"

    def setupSpec() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider())
        }
    }

    private JasyptEncryptionAliasPlugin createPlugin(String password = TEST_PASSWORD) {
        def plugin = new JasyptEncryptionAliasPlugin()
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

    // --- Plugin registration ---

    def "alias plugin registers under jasypt-encryption provider name"() {
        when:
        def annotation = JasyptEncryptionAliasPlugin.getAnnotation(Plugin)

        then:
        annotation.name() == "jasypt-encryption"
        annotation.service() == ServiceNameConstants.StorageConverter
    }

    def "alias plugin is a subclass of ModernEncryptionConverterPlugin"() {
        expect:
        ModernEncryptionConverterPlugin.isAssignableFrom(JasyptEncryptionAliasPlugin)
    }

    // --- Functional equivalence: encrypt/decrypt round-trip ---

    def "alias plugin encrypts and decrypts via createResource/readResource round-trip"() {
        given:
        def plugin = createPlugin()
        def plaintext = "alias round-trip test".bytes
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
        createMeta.getResourceMeta()["aes-gcm-encryption:encrypted"] == "true"
    }

    // --- Dual-read: alias can read legacy Jasypt-encrypted data ---

    def "alias plugin reads legacy jasypt-encrypted data"() {
        given:
        def plugin = createPlugin()
        def plaintext = "jasypt legacy via alias".bytes
        def path = Mock(Path)

        and:
        def jasyptEncrypted = new LegacyJasyptEncryptor(
                "PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000
        ).encrypt(TEST_PASSWORD, plaintext)

        and:
        def meta = metaWith(["jasypt-encryption:encrypted": "true"])

        when:
        def result = plugin.readResource(path, meta, mockHasInputStream(jasyptEncrypted))

        then:
        readAllBytes(result) == plaintext
    }

    // --- Cross-compatibility: data encrypted by alias is readable by parent plugin ---

    def "data encrypted by alias plugin is decryptable by ModernEncryptionConverterPlugin"() {
        given:
        def aliasPlugin = createPlugin()
        def modernPlugin = new ModernEncryptionConverterPlugin()
        modernPlugin.password = TEST_PASSWORD
        modernPlugin.legacyEncryptorType = "custom"
        def plaintext = "cross-compat test".bytes
        def path = Mock(Path)

        and:
        def createMeta = metaWith([:])
        def encrypted = aliasPlugin.createResource(path, createMeta, mockHasInputStream(plaintext))
        def encryptedBytes = readAllBytes(encrypted)

        and:
        def readMeta = metaWith(createMeta.getResourceMeta())

        when:
        def decrypted = modernPlugin.readResource(path, readMeta, mockHasInputStream(encryptedBytes))

        then:
        readAllBytes(decrypted) == plaintext
    }

    def "data encrypted by ModernEncryptionConverterPlugin is decryptable by alias plugin"() {
        given:
        def modernPlugin = new ModernEncryptionConverterPlugin()
        modernPlugin.password = TEST_PASSWORD
        modernPlugin.legacyEncryptorType = "custom"
        def aliasPlugin = createPlugin()
        def plaintext = "reverse cross-compat test".bytes
        def path = Mock(Path)

        and:
        def createMeta = metaWith([:])
        def encrypted = modernPlugin.createResource(path, createMeta, mockHasInputStream(plaintext))
        def encryptedBytes = readAllBytes(encrypted)

        and:
        def readMeta = metaWith(createMeta.getResourceMeta())

        when:
        def decrypted = aliasPlugin.readResource(path, readMeta, mockHasInputStream(encryptedBytes))

        then:
        readAllBytes(decrypted) == plaintext
    }

    // --- Migration lifecycle via alias ---

    def "full migration lifecycle through alias: read jasypt data, update to modern, read modern"() {
        given:
        def plugin = createPlugin()
        def plaintext = "migration via alias".bytes
        def path = Mock(Path)

        and:
        def jasyptEncrypted = new LegacyJasyptEncryptor(
                "PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000
        ).encrypt(TEST_PASSWORD, plaintext)

        when: "read jasypt-encrypted data through alias"
        def readMeta = metaWith(["jasypt-encryption:encrypted": "true"])
        def decrypted = plugin.readResource(path, readMeta, mockHasInputStream(jasyptEncrypted))

        then:
        readAllBytes(decrypted) == plaintext

        when: "update re-encrypts with AES-GCM"
        def updateMeta = metaWith(["jasypt-encryption:encrypted": "true"])
        def modernEncrypted = plugin.updateResource(path, updateMeta, mockHasInputStream(plaintext))
        def modernEncryptedBytes = readAllBytes(modernEncrypted)

        then:
        updateMeta.getResourceMeta()["aes-gcm-encryption:encrypted"] == "true"
        updateMeta.getResourceMeta()["jasypt-encryption:encrypted"] == "false"

        when: "read the re-encrypted data"
        def readMeta2 = metaWith(updateMeta.getResourceMeta())
        def decrypted2 = plugin.readResource(path, readMeta2, mockHasInputStream(modernEncryptedBytes))

        then:
        readAllBytes(decrypted2) == plaintext
    }
}
