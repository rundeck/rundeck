package org.rundeck.plugin.encryption

import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.rundeck.storage.api.HasInputStream
import org.rundeck.storage.api.Path
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.lang.Unroll

import java.security.Security

/**
 * Integration tests proving backward compatibility between the new AES-GCM encryption
 * plugin and data encrypted by the old Jasypt plugin.
 *
 * <p>These tests simulate what happens when a Rundeck instance is upgraded from a version
 * using jasypt-encryption to the new aes-gcm-encryption plugin:
 * <ul>
 *   <li>Pre-existing Jasypt-encrypted data can be read without modification</li>
 *   <li>The jasypt-encryption alias plugin works identically to the main plugin</li>
 *   <li>Updated data is re-encrypted with AES-256-GCM (lazy migration)</li>
 *   <li>After migration, data remains readable</li>
 *   <li>Multiple data types (passwords, private keys, certificates) are handled</li>
 * </ul>
 *
 * <p>Fixtures are generated using {@link LegacyJasyptEncryptor} which produces output
 * bit-for-bit compatible with Jasypt's {@code StandardPBEByteEncryptor} (verified
 * independently in {@code LegacyJasyptEncryptorSpec}).
 */
@Stepwise
class BackwardCompatibilityIntegrationSpec extends Specification {

    static final String PRODUCTION_PASSWORD = "rundeck-production-encryption-key-2024"

    @Shared Map<String, byte[]> jasyptFixtures = [:]
    @Shared Map<String, byte[]> plaintexts = [:]

    def setupSpec() {
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider())
        }

        plaintexts = [
            "simple-password"  : "my-secret-database-password-123".bytes,
            "ssh-private-key"  : generateFakePrivateKey(),
            "api-token"        : "tk-a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6".bytes,
            "empty-value"      : new byte[0],
            "unicode-password" : "contraseña-日本語-пароль".bytes,
            "large-certificate": generateFakeCertificate(),
        ]

        def encryptor = LegacyJasyptEncryptor.defaultStorage()
        plaintexts.each { name, plaintext ->
            jasyptFixtures[name] = encryptor.encrypt(PRODUCTION_PASSWORD, plaintext)
        }
    }

    // ========================================================================
    // SCENARIO 1: Post-upgrade read of Jasypt-encrypted data via aes-gcm-encryption
    // ========================================================================

    @Unroll
    def "post-upgrade: aes-gcm-encryption plugin reads Jasypt-encrypted '#name'"() {
        given: "a plugin configured as aes-gcm-encryption (new installs)"
        def plugin = createModernPlugin()
        def path = Mock(Path)
        def meta = metaWith(["jasypt-encryption:encrypted": "true"])

        when: "reading pre-existing Jasypt-encrypted data"
        def result = plugin.readResource(path, meta, mockStream(jasyptFixtures[name]))

        then: "plaintext is recovered correctly"
        readAllBytes(result) == plaintexts[name]

        where:
        name << ["simple-password", "ssh-private-key", "api-token",
                 "empty-value", "unicode-password", "large-certificate"]
    }

    // ========================================================================
    // SCENARIO 2: Post-upgrade read via jasypt-encryption ALIAS plugin
    // ========================================================================

    @Unroll
    def "post-upgrade: jasypt-encryption alias reads Jasypt-encrypted '#name'"() {
        given: "a plugin loaded via the jasypt-encryption alias (existing config, no changes)"
        def plugin = createAliasPlugin()
        def path = Mock(Path)
        def meta = metaWith(["jasypt-encryption:encrypted": "true"])

        when: "reading pre-existing Jasypt-encrypted data through the alias"
        def result = plugin.readResource(path, meta, mockStream(jasyptFixtures[name]))

        then: "plaintext is recovered correctly"
        readAllBytes(result) == plaintexts[name]

        where:
        name << ["simple-password", "ssh-private-key", "api-token",
                 "empty-value", "unicode-password", "large-certificate"]
    }

    // ========================================================================
    // SCENARIO 3: Lazy migration -- update re-encrypts with AES-GCM
    // ========================================================================

    @Unroll
    def "lazy migration: updating '#name' re-encrypts from Jasypt to AES-GCM"() {
        given: "a plugin with Jasypt-encrypted data"
        def plugin = createModernPlugin()
        def path = Mock(Path)

        and: "read the old data first"
        def readMeta = metaWith(["jasypt-encryption:encrypted": "true"])
        def decrypted = plugin.readResource(path, readMeta, mockStream(jasyptFixtures[name]))
        def originalPlaintext = readAllBytes(decrypted)

        when: "the resource is updated (triggers lazy migration)"
        def updateMeta = metaWith(["jasypt-encryption:encrypted": "true"])
        def reEncrypted = plugin.updateResource(path, updateMeta, mockStream(originalPlaintext))
        def reEncryptedBytes = readAllBytes(reEncrypted)

        then: "metadata reflects migration to AES-GCM"
        updateMeta.getResourceMeta()["aes-gcm-encryption:encrypted"] == "true"
        updateMeta.getResourceMeta()["jasypt-encryption:encrypted"] == "false"

        and: "the data is now in AES-GCM format (version byte 0x01)"
        reEncryptedBytes[0] == AesEncryptor.FORMAT_VERSION

        and: "the re-encrypted data can be read back"
        def readMeta2 = metaWith(updateMeta.getResourceMeta())
        def finalResult = plugin.readResource(path, readMeta2, mockStream(reEncryptedBytes))
        readAllBytes(finalResult) == plaintexts[name]

        where:
        name << ["simple-password", "ssh-private-key", "api-token",
                 "empty-value", "unicode-password", "large-certificate"]
    }

    // ========================================================================
    // SCENARIO 4: Cross-plugin compatibility (alias writes, modern reads)
    // ========================================================================

    def "cross-compat: data written via alias is readable by aes-gcm-encryption"() {
        given:
        def aliasPlugin = createAliasPlugin()
        def modernPlugin = createModernPlugin()
        def plaintext = "cross-plugin secret".bytes
        def path = Mock(Path)

        when: "alias plugin creates a resource"
        def createMeta = metaWith([:])
        def encrypted = aliasPlugin.createResource(path, createMeta, mockStream(plaintext))
        def encryptedBytes = readAllBytes(encrypted)

        then: "modern plugin can read it"
        def readMeta = metaWith(createMeta.getResourceMeta())
        def result = modernPlugin.readResource(path, readMeta, mockStream(encryptedBytes))
        readAllBytes(result) == plaintext
    }

    def "cross-compat: data written via aes-gcm-encryption is readable by alias"() {
        given:
        def aliasPlugin = createAliasPlugin()
        def modernPlugin = createModernPlugin()
        def plaintext = "reverse cross-plugin secret".bytes
        def path = Mock(Path)

        when: "modern plugin creates a resource"
        def createMeta = metaWith([:])
        def encrypted = modernPlugin.createResource(path, createMeta, mockStream(plaintext))
        def encryptedBytes = readAllBytes(encrypted)

        then: "alias plugin can read it"
        def readMeta = metaWith(createMeta.getResourceMeta())
        def result = aliasPlugin.readResource(path, readMeta, mockStream(encryptedBytes))
        readAllBytes(result) == plaintext
    }

    // ========================================================================
    // SCENARIO 5: Different legacy algorithm configurations
    // ========================================================================

    def "backward compat: reads data from Jasypt 'basic' mode (PBEWithMD5AndDES)"() {
        given:
        def plaintext = "basic mode secret".bytes
        def encryptor = new LegacyJasyptEncryptor("PBEWithMD5AndDES", "BC", 1000)
        def encrypted = encryptor.encrypt(PRODUCTION_PASSWORD, plaintext)

        and: "plugin configured for basic legacy type"
        def plugin = new ModernEncryptionConverterPlugin()
        plugin.password = PRODUCTION_PASSWORD
        plugin.legacyEncryptorType = "basic"

        def path = Mock(Path)
        def meta = metaWith(["jasypt-encryption:encrypted": "true"])

        when:
        def result = plugin.readResource(path, meta, mockStream(encrypted))

        then:
        readAllBytes(result) == plaintext
    }

    def "backward compat: reads data from 256-bit AES custom config"() {
        given:
        def plaintext = "256-bit aes secret".bytes
        def encryptor = new LegacyJasyptEncryptor("PBEWITHSHA256AND256BITAES-CBC-BC", "BC", 1000)
        def encrypted = encryptor.encrypt(PRODUCTION_PASSWORD, plaintext)

        and: "plugin configured with 256-bit algorithm override"
        def plugin = new ModernEncryptionConverterPlugin()
        plugin.password = PRODUCTION_PASSWORD
        plugin.legacyEncryptorType = "custom"
        plugin.legacyAlgorithm = "PBEWITHSHA256AND256BITAES-CBC-BC"
        plugin.legacyProvider = "BC"

        def path = Mock(Path)
        def meta = metaWith(["jasypt-encryption:encrypted": "true"])

        when:
        def result = plugin.readResource(path, meta, mockStream(encrypted))

        then:
        readAllBytes(result) == plaintext
    }

    // ========================================================================
    // SCENARIO 6: Full lifecycle proving no data loss during upgrade
    // ========================================================================

    def "full upgrade lifecycle: multiple keys survive jasypt → aes-gcm migration"() {
        given: "simulate a storage tree with multiple Jasypt-encrypted keys"
        def plugin = createModernPlugin()
        def path = Mock(Path)

        and: "store all our fixtures as if they were in the DB"
        def storedData = jasyptFixtures.collectEntries { name, encrypted ->
            [name, [data: encrypted, meta: ["jasypt-encryption:encrypted": "true"]]]
        }

        when: "read all keys (simulating post-upgrade first access)"
        def readResults = storedData.collectEntries { name, stored ->
            def meta = metaWith(stored.meta as Map<String, String>)
            def result = plugin.readResource(path, meta, mockStream(stored.data as byte[]))
            [name, readAllBytes(result)]
        }

        then: "all keys are readable and match original plaintext"
        readResults.each { name, decrypted ->
            assert decrypted == plaintexts[name]: "Failed for key: ${name}"
        }

        when: "update all keys (simulating lazy migration)"
        def migratedData = storedData.collectEntries { name, stored ->
            def updateMeta = metaWith(stored.meta as Map<String, String>)
            def reEncrypted = plugin.updateResource(path, updateMeta, mockStream(plaintexts[name]))
            [name, [data: readAllBytes(reEncrypted), meta: updateMeta.getResourceMeta()]]
        }

        then: "all migrated keys are in AES-GCM format and readable"
        migratedData.each { name, migrated ->
            assert (migrated.data as byte[])[0] == AesEncryptor.FORMAT_VERSION:
                "Key '${name}' not in AES-GCM format after migration"

            def readMeta = metaWith(migrated.meta as Map<String, String>)
            def result = plugin.readResource(path, readMeta, mockStream(migrated.data as byte[]))
            assert readAllBytes(result) == plaintexts[name]:
                "Key '${name}' data corrupted after migration"
        }
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    private ModernEncryptionConverterPlugin createModernPlugin() {
        def plugin = new ModernEncryptionConverterPlugin()
        plugin.password = PRODUCTION_PASSWORD
        plugin.legacyEncryptorType = "custom"
        return plugin
    }

    private JasyptEncryptionAliasPlugin createAliasPlugin() {
        def plugin = new JasyptEncryptionAliasPlugin()
        plugin.password = PRODUCTION_PASSWORD
        plugin.legacyEncryptorType = "custom"
        return plugin
    }

    private ResourceMetaBuilder metaWith(Map<String, String> entries) {
        def meta = new ResourceMetaBuilder()
        entries.each { k, v -> meta.getResourceMeta().put(k, v) }
        return meta
    }

    private HasInputStream mockStream(byte[] data) {
        return new HasInputStream() {
            @Override
            InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(data)
            }

            @Override
            long writeContent(OutputStream outputStream) throws IOException {
                def is = getInputStream()
                def buf = new byte[4096]
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

    private static byte[] generateFakePrivateKey() {
        def sb = new StringBuilder()
        sb.append("-----BEGIN RSA PRIVATE KEY-----\n")
        def random = new Random(12345)
        10.times {
            def line = new byte[48]
            random.nextBytes(line)
            sb.append(Base64.encoder.encodeToString(line)).append("\n")
        }
        sb.append("-----END RSA PRIVATE KEY-----\n")
        return sb.toString().bytes
    }

    private static byte[] generateFakeCertificate() {
        def sb = new StringBuilder()
        sb.append("-----BEGIN CERTIFICATE-----\n")
        def random = new Random(67890)
        30.times {
            def line = new byte[48]
            random.nextBytes(line)
            sb.append(Base64.encoder.encodeToString(line)).append("\n")
        }
        sb.append("-----END CERTIFICATE-----\n")
        return sb.toString().bytes
    }
}
