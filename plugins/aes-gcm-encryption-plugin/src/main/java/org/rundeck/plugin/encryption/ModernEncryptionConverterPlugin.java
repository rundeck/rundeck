/*
 * Copyright 2026 PagerDuty, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.rundeck.plugin.encryption;

import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.storage.ResourceMetaBuilder;
import com.dtolabs.rundeck.plugins.ServiceNameConstants;
import com.dtolabs.rundeck.plugins.descriptions.Password;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.descriptions.SelectValues;
import com.dtolabs.rundeck.plugins.storage.StorageConverterPlugin;
import com.dtolabs.utils.Streams;
import org.rundeck.storage.api.HasInputStream;
import org.rundeck.storage.api.Path;
import org.rundeck.storage.data.DataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * AES-GCM storage encryption plugin using AES-256-GCM with PBKDF2 key derivation.
 *
 * <p>Provides dual-read capability: can decrypt both AES-GCM and
 * legacy Jasypt-encrypted data. New writes always use AES-256-GCM. This enables
 * transparent lazy migration — existing Jasypt-encrypted items are re-encrypted
 * with AES-256-GCM on their next update.
 *
 * <p>Drop-in replacement for {@code jasypt-encryption} in
 * {@code rundeck-config.properties} converter configuration.
 */
@Plugin(name = ModernEncryptionConverterPlugin.PROVIDER_NAME, service = ServiceNameConstants.StorageConverter)
@PluginDescription(
        title = "AES-GCM Storage Encryption",
        description = "Encrypts data in the Rundeck Storage layer using AES-256-GCM with PBKDF2 key derivation.\n\n" +
                "This plugin replaces jasypt-encryption with authenticated encryption (AES-GCM). " +
                "It can transparently read data encrypted by the old Jasypt plugin (dual-read), " +
                "and all new writes use AES-256-GCM.\n\n" +
                "Password can be specified directly, via environment variable, or via Java system property."
)
public class ModernEncryptionConverterPlugin implements StorageConverterPlugin {

    public static final String PROVIDER_NAME = "aes-gcm-encryption";
    public static final String JASYPT_PROVIDER_NAME = "jasypt-encryption";
    public static final String META_ENCRYPTED = PROVIDER_NAME + ":encrypted";
    public static final String JASYPT_META_ENCRYPTED = JASYPT_PROVIDER_NAME + ":encrypted";

    private static final Logger logger = LoggerFactory.getLogger(ModernEncryptionConverterPlugin.class);

    @PluginProperty(
            title = "Password",
            description = "Encryption password (same password used by the previous Jasypt plugin)",
            required = false
    )
    @Password
    String password;

    @PluginProperty(
            title = "Password Environment Variable",
            description = "Name of environment variable storing the encryption password",
            required = false
    )
    String passwordEnvVarName;

    @PluginProperty(
            title = "Password System Property",
            description = "Name of JVM system property storing the encryption password",
            required = false
    )
    String passwordSysPropName;

    @PluginProperty(
            name = "encryptorType",
            title = "Legacy Encryptor Type",
            description = "Jasypt encryptor type used previously.\n\n" +
                    "* 'custom' — algorithm PBEWITHSHA256AND128BITAES-CBC-BC with BC provider (Rundeck default)\n" +
                    "* 'basic' — algorithm PBEWithMD5AndDES\n\n" +
                    "Only needed for reading existing Jasypt-encrypted data.",
            defaultValue = "custom",
            required = false
    )
    @SelectValues(values = {"custom", "basic"})
    String legacyEncryptorType;

    @PluginProperty(
            name = "algorithm",
            title = "Legacy Algorithm",
            description = "(optional) Override the legacy Jasypt algorithm. Only used when legacyEncryptorType is 'custom'.",
            required = false
    )
    String legacyAlgorithm;

    @PluginProperty(
            name = "provider",
            title = "Legacy Provider",
            description = "(optional) Override the legacy JCE provider name for Jasypt decryption. Default: 'BC'.",
            required = false
    )
    String legacyProvider;

    @PluginProperty(
            name = "keyObtentionIterations",
            title = "Legacy Key Obtention Iterations",
            description = "(optional) Number of PBE iterations used by the previous Jasypt config. Default: 1000.",
            required = false
    )
    String legacyKeyObtentionIterations;

    private volatile AesEncryptor aesEncryptor;
    private volatile LegacyJasyptDecryptor legacyDecryptor;
    private volatile char[] resolvedPassword;

    @Override
    public HasInputStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
                                       HasInputStream hasInputStream) {
        boolean jasyptEncrypted = "true".equals(resourceMetaBuilder.getResourceMeta().get(JASYPT_META_ENCRYPTED));
        boolean aesEncrypted = "true".equals(resourceMetaBuilder.getResourceMeta().get(META_ENCRYPTED));

        // The two flags are mutually exclusive by construction: every AES-GCM write clears the
        // legacy Jasypt flag (see createResource/updateResource), and content + metadata are
        // persisted atomically by the storage layer (single row write). Therefore any record
        // that still carries jasypt-encryption:encrypted=true holds Jasypt-encrypted content —
        // even when the aes-gcm-encryption:encrypted flag is also true. The legacy flag wins so
        // that inconsistent records (e.g. production row id=130, where both flags were true) are
        // decrypted with the correct algorithm instead of crashing. See RUN-4512.
        if (jasyptEncrypted) {
            if (aesEncrypted) {
                logger.warn("readResource: record at '{}' has both encryption flags set; decrypting as "
                        + "legacy Jasypt (content predates the AES-GCM migration, see RUN-4512). It will be "
                        + "re-encrypted to AES-GCM on its next update.", path);
            } else {
                logger.debug("readResource (jasypt-encrypted, legacy) {}", path);
            }
            return decryptLegacy(path, hasInputStream);
        }
        if (aesEncrypted) {
            logger.debug("readResource (aes-gcm-encrypted) {}", path);
            return decryptModern(hasInputStream);
        }
        logger.debug("readResource (unencrypted) {}", path);
        return null;
    }

    @Override
    public HasInputStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
                                         HasInputStream hasInputStream) {
        resourceMetaBuilder.getResourceMeta().put(META_ENCRYPTED, "true");
        resourceMetaBuilder.getResourceMeta().put(JASYPT_META_ENCRYPTED, "false");
        logger.debug("createResource {}", path);
        return encrypt(hasInputStream);
    }

    @Override
    public HasInputStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
                                         HasInputStream hasInputStream) {
        resourceMetaBuilder.getResourceMeta().put(JASYPT_META_ENCRYPTED, "false");
        resourceMetaBuilder.getResourceMeta().put(META_ENCRYPTED, "true");
        logger.debug("updateResource {}", path);
        return encrypt(hasInputStream);
    }

    private HasInputStream encrypt(HasInputStream input) {
        try {
            return new TransformStream(input) {
                @Override
                protected byte[] transform(byte[] data) {
                    return getAesEncryptor().encrypt(getResolvedPassword(), data);
                }
            };
        } catch (Exception e) {
            logger.error("AES-256-GCM encryption failed. Check encryption password configuration.", e);
            throw new RuntimeException("AES-256-GCM encryption failed", e);
        }
    }

    private HasInputStream decryptModern(HasInputStream input) {
        try {
            return new TransformStream(input) {
                @Override
                protected byte[] transform(byte[] data) {
                    return getAesEncryptor().decrypt(getResolvedPassword(), data);
                }
            };
        } catch (Exception e) {
            logger.error("AES-256-GCM decryption failed. Wrong password or corrupted data.", e);
            throw new RuntimeException("AES-256-GCM decryption failed", e);
        }
    }

    private HasInputStream decryptLegacy(Path path, HasInputStream input) {
        try {
            return new TransformStream(input) {
                @Override
                protected byte[] transform(byte[] data) {
                    if (looksLikeProjectPropertiesPlaintext(path, data)) {
                        // A pre-existing 5.x condition: the jasypt-encryption:encrypted flag was set
                        // on this record, but the content was never actually encrypted. Checked BEFORE
                        // attempting decryption (not just as a post-failure fallback): legacy CBC is
                        // unauthenticated, so decrypting these plaintext bytes could occasionally
                        // "succeed" with garbage instead of throwing (if their length happens to be
                        // block-aligned), which would skip a post-failure check entirely. The exact
                        // path + full header match below is specific enough to check safely up front.
                        if (plaintextFallbackWarnedPaths.add(String.valueOf(path))) {
                            logger.warn("readResource: content flagged jasypt-encryption:encrypted=true "
                                    + "at '{}' looks like plaintext; returning raw content unchanged "
                                    + "without attempting to decrypt it.", path);
                        } else {
                            logger.debug("readResource: returning raw plaintext content for '{}' "
                                    + "(already warned).", path);
                        }
                        return data;
                    }
                    return getLegacyDecryptor().decrypt(getResolvedPassword(), data);
                }
            };
        } catch (Exception e) {
            logger.error("Legacy Jasypt decryption failed. Wrong password or incompatible algorithm.", e);
            throw new RuntimeException("Legacy Jasypt decryption failed", e);
        }
    }

    /**
     * Records paths for which the plaintext-fallback warning has already been logged, so a resource
     * re-read on every project-cache refresh (e.g. every minute) doesn't log a fresh WARNING each time.
     */
    private final Set<String> plaintextFallbackWarnedPaths = ConcurrentHashMap.newKeySet();

    /**
     * Matches exactly {@code projects/<name>/etc/project.properties} -- one non-empty path segment
     * for the project name, anchored at both ends -- so it can never match an unrelated storage path
     * that merely ends the same way, e.g. a Key Storage entry at {@code keys/foo/etc/project.properties}
     * (this same plugin class is also configured for Key Storage; see {@code JASYPT_PROVIDER_NAME}
     * usage in {@code rundeck-config.properties}). The plaintext-recovery fallback below is
     * deliberately restricted to this one real path: it must never apply to SCM config, Key Storage,
     * or any other legacy-flagged resource.
     */
    private static final Pattern PROJECT_PROPERTIES_PATH_PATTERN =
            Pattern.compile("^projects/[^/]+/etc/project\\.properties$");

    /**
     * The exact header {@code java.util.Properties.store()} writes as its first comment line,
     * matching {@code ProjectManagerService.isValidConfigFile()}'s own check
     * ({@code '#' + MIME_TYPE_PROJECT_PROPERTIES}). Requiring this full header -- not just a leading
     * {@code '#'} -- combined with the exact-path match above, makes this check specific enough to
     * run safely before ever attempting decryption.
     */
    private static final byte[] PROJECT_PROPERTIES_HEADER =
            "#text/x-java-properties".getBytes(StandardCharsets.ISO_8859_1);

    /**
     * Detects a pre-existing 5.x condition: {@code project.properties} stored as genuine plaintext
     * (a real {@code Properties.store()} header) while still carrying a stale
     * {@code jasypt-encryption:encrypted=true} flag. Requires an exact match on both the real project-
     * config path and the full header -- not just a leading {@code '#'} -- which is specific enough
     * that this can run before attempting decryption at all, rather than only as a post-failure
     * fallback: a byte {@code 0x23} ('#') alone is a perfectly valid (if unlikely, ~1/256) first byte
     * of this decryptor's random salt for genuine ciphertext, but the full header plus the exact path
     * together make an accidental match on real ciphertext astronomically unlikely. This must never
     * apply to SCM config, Key Storage, or any other legacy-flagged resource, where a still-
     * undecryptable payload must keep failing loudly instead of being returned as "plaintext" and
     * potentially persisted as such on the next write.
     */
    private static boolean looksLikeProjectPropertiesPlaintext(Path path, byte[] data) {
        if (data == null || data.length < PROJECT_PROPERTIES_HEADER.length) {
            return false;
        }
        String pathString = path != null ? path.getPath() : null;
        if (pathString == null || !PROJECT_PROPERTIES_PATH_PATTERN.matcher(pathString).matches()) {
            return false;
        }
        for (int i = 0; i < PROJECT_PROPERTIES_HEADER.length; i++) {
            if (data[i] != PROJECT_PROPERTIES_HEADER[i]) {
                return false;
            }
        }
        return true;
    }

    char[] getResolvedPassword() {
        if (resolvedPassword == null) {
            synchronized (this) {
                if (resolvedPassword == null) {
                    resolvedPassword = resolvePassword();
                }
            }
        }
        return resolvedPassword;
    }

    private char[] resolvePassword() {
        if (notBlank(password)) {
            char[] pw = password.toCharArray();
            password = null;
            return pw;
        }
        if (notBlank(passwordEnvVarName)) {
            String envVal = System.getenv(passwordEnvVarName);
            passwordEnvVarName = null;
            if (notBlank(envVal)) {
                return envVal.toCharArray();
            }
        }
        if (notBlank(passwordSysPropName)) {
            String propVal = System.getProperty(passwordSysPropName);
            System.clearProperty(passwordSysPropName);
            passwordSysPropName = null;
            if (notBlank(propVal)) {
                return propVal.toCharArray();
            }
        }
        throw new IllegalStateException(
                "Encryption password is required. Set password, passwordEnvVarName, or passwordSysPropName."
        );
    }

    AesEncryptor getAesEncryptor() {
        if (aesEncryptor == null) {
            synchronized (this) {
                if (aesEncryptor == null) {
                    aesEncryptor = new AesEncryptor();
                    logger.debug("AesEncryptor (AES-256-GCM) initialized");
                }
            }
        }
        return aesEncryptor;
    }

    LegacyJasyptDecryptor getLegacyDecryptor() {
        if (legacyDecryptor == null) {
            synchronized (this) {
                if (legacyDecryptor == null) {
                    legacyDecryptor = buildLegacyDecryptor();
                    logger.debug("LegacyJasyptDecryptor initialized (type={}, algorithm={})",
                            legacyEncryptorType,
                            legacyDecryptor != null ? "configured" : "none");
                }
            }
        }
        return legacyDecryptor;
    }

    private LegacyJasyptDecryptor buildLegacyDecryptor() {
        String algo;
        String prov;
        int iterations;

        if ("basic".equals(legacyEncryptorType)) {
            algo = "PBEWithMD5AndDES";
            prov = "BC";
            iterations = 1000;
        } else {
            algo = notBlank(legacyAlgorithm) ? legacyAlgorithm : "PBEWITHSHA256AND128BITAES-CBC-BC";
            prov = notBlank(legacyProvider) ? legacyProvider : "BC";
            iterations = parseLegacyIterations();
        }

        return new LegacyJasyptDecryptor(algo, prov, iterations);
    }

    private int parseLegacyIterations() {
        if (notBlank(legacyKeyObtentionIterations)) {
            try {
                return Integer.parseInt(legacyKeyObtentionIterations);
            } catch (NumberFormatException e) {
                logger.warn("Invalid legacyKeyObtentionIterations '{}', using default 1000",
                        legacyKeyObtentionIterations);
            }
        }
        return 1000;
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isEmpty();
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        Streams.copyStream(inputStream, buffer);
        return buffer.toByteArray();
    }

    /**
     * Lazily transforms the underlying stream content. The actual
     * encrypt/decrypt happens when {@code getInputStream()} is called,
     * not at construction time.
     */
    private abstract static class TransformStream implements HasInputStream {
        private final HasInputStream source;

        TransformStream(HasInputStream source) {
            this.source = source;
        }

        protected abstract byte[] transform(byte[] data);

        @Override
        public InputStream getInputStream() throws IOException {
            byte[] raw = readAllBytes(source.getInputStream());
            return new ByteArrayInputStream(transform(raw));
        }

        @Override
        public long writeContent(OutputStream outputStream) throws IOException {
            return DataUtil.copyStream(getInputStream(), outputStream);
        }
    }
}
