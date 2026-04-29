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

/**
 * Modern encryption plugin using AES-256-GCM with PBKDF2 key derivation.
 *
 * <p>Provides dual-read capability: can decrypt both modern (AES-256-GCM) and
 * legacy Jasypt-encrypted data. New writes always use AES-256-GCM. This enables
 * transparent lazy migration — existing Jasypt-encrypted items are re-encrypted
 * with AES-256-GCM on their next update.
 *
 * <p>Drop-in replacement for {@code jasypt-encryption} in
 * {@code rundeck-config.properties} converter configuration.
 */
@Plugin(name = ModernEncryptionConverterPlugin.PROVIDER_NAME, service = ServiceNameConstants.StorageConverter)
@PluginDescription(
        title = "Modern Encryption (AES-256-GCM)",
        description = "Encrypts data in the Rundeck Storage layer using AES-256-GCM with PBKDF2 key derivation.\n\n" +
                "This plugin replaces jasypt-encryption with modern authenticated encryption. " +
                "It can transparently read data encrypted by the old Jasypt plugin (dual-read), " +
                "and all new writes use AES-256-GCM.\n\n" +
                "Password can be specified directly, via environment variable, or via Java system property."
)
public class ModernEncryptionConverterPlugin implements StorageConverterPlugin {

    public static final String PROVIDER_NAME = "modern-encryption";
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
            title = "Legacy Algorithm",
            description = "(optional) Override the legacy Jasypt algorithm. Only used when legacyEncryptorType is 'custom'.",
            required = false
    )
    String legacyAlgorithm;

    @PluginProperty(
            title = "Legacy Provider",
            description = "(optional) Override the legacy JCE provider name for Jasypt decryption. Default: 'BC'.",
            required = false
    )
    String legacyProvider;

    @PluginProperty(
            title = "Legacy Key Obtention Iterations",
            description = "(optional) Number of PBE iterations used by the previous Jasypt config. Default: 1000.",
            required = false
    )
    String legacyKeyObtentionIterations;

    private volatile ModernEncryptor modernEncryptor;
    private volatile LegacyJasyptDecryptor legacyDecryptor;
    private volatile char[] resolvedPassword;

    @Override
    public HasInputStream readResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
                                       HasInputStream hasInputStream) {
        if ("true".equals(resourceMetaBuilder.getResourceMeta().get(META_ENCRYPTED))) {
            logger.debug("readResource (modern-encrypted) {}", path);
            return decryptModern(hasInputStream);
        }
        if ("true".equals(resourceMetaBuilder.getResourceMeta().get(JASYPT_META_ENCRYPTED))) {
            logger.debug("readResource (jasypt-encrypted, legacy fallback) {}", path);
            return decryptLegacy(hasInputStream);
        }
        logger.debug("readResource (unencrypted) {}", path);
        return null;
    }

    @Override
    public HasInputStream createResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
                                         HasInputStream hasInputStream) {
        resourceMetaBuilder.getResourceMeta().put(META_ENCRYPTED, "true");
        logger.debug("createResource {}", path);
        return encryptModern(hasInputStream);
    }

    @Override
    public HasInputStream updateResource(Path path, ResourceMetaBuilder resourceMetaBuilder,
                                         HasInputStream hasInputStream) {
        resourceMetaBuilder.getResourceMeta().put(JASYPT_META_ENCRYPTED,"false");
        resourceMetaBuilder.getResourceMeta().put(META_ENCRYPTED, "true");
        logger.debug("updateResource {}", path);
        return encryptModern(hasInputStream);
    }

    private HasInputStream encryptModern(HasInputStream input) {
        try {
            return new TransformStream(input) {
                @Override
                protected byte[] transform(byte[] data) {
                    return getModernEncryptor().encrypt(getResolvedPassword(), data);
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
                    return getModernEncryptor().decrypt(getResolvedPassword(), data);
                }
            };
        } catch (Exception e) {
            logger.error("AES-256-GCM decryption failed. Wrong password or tampered data.", e);
            throw new RuntimeException("AES-256-GCM decryption failed", e);
        }
    }

    private HasInputStream decryptLegacy(HasInputStream input) {
        try {
            return new TransformStream(input) {
                @Override
                protected byte[] transform(byte[] data) {
                    return getLegacyDecryptor().decrypt(getResolvedPassword(), data);
                }
            };
        } catch (Exception e) {
            logger.error("Legacy Jasypt decryption failed. Wrong password or incompatible algorithm.", e);
            throw new RuntimeException("Legacy Jasypt decryption failed", e);
        }
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
            passwordSysPropName = null;
            if (notBlank(propVal)) {
                return propVal.toCharArray();
            }
        }
        throw new IllegalStateException(
                "Encryption password is required. Set password, passwordEnvVarName, or passwordSysPropName."
        );
    }

    ModernEncryptor getModernEncryptor() {
        if (modernEncryptor == null) {
            synchronized (this) {
                if (modernEncryptor == null) {
                    modernEncryptor = new ModernEncryptor();
                    logger.debug("ModernEncryptor (AES-256-GCM) initialized");
                }
            }
        }
        return modernEncryptor;
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
