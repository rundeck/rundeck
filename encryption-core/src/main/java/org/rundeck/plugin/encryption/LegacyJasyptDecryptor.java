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

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.Security;
import java.util.Arrays;

/**
 * Decrypts data produced by Jasypt's {@code StandardPBEByteEncryptor} without
 * requiring the Jasypt library.
 *
 * <p>Jasypt output format (with default {@code RandomSaltGenerator} and {@code NoIvGenerator}):
 * <pre>
 *   [salt : blockSize bytes] [ciphertext]
 * </pre>
 * Where {@code blockSize} is the cipher's block size (16 for AES, 8 for DES).
 * The salt is prepended unencrypted. No separate IV is stored; BC derives it
 * from the PBE key derivation process.
 *
 * <p>Supported algorithms (matching Rundeck production usage):
 * <ul>
 *   <li>{@code PBEWITHSHA256AND128BITAES-CBC-BC} — storage keys/project config (salt=16)</li>
 *   <li>{@code PBEWITHSHA256AND256BITAES-CBC-BC} — encrypt-datasource-password (salt=16)</li>
 *   <li>{@code PBEWithMD5AndDES} — encryptable-core-properties (salt=8)</li>
 *   <li>{@code PBEWithMD5AndTripleDES} — jasypt "strong" mode (salt=8)</li>
 * </ul>
 */
public class LegacyJasyptDecryptor {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final String algorithm;
    private final String provider;
    private final int keyObtentionIterations;
    private final int saltSizeBytes;

    /**
     * @param algorithm              PBE algorithm name (e.g. {@code PBEWITHSHA256AND128BITAES-CBC-BC})
     * @param provider               JCE provider name (e.g. {@code "BC"}) or {@code null} for default
     * @param keyObtentionIterations number of PBE hashing iterations (Jasypt default: 1000)
     */
    public LegacyJasyptDecryptor(String algorithm, String provider, int keyObtentionIterations) {
        this.algorithm = algorithm;
        this.provider = provider;
        this.keyObtentionIterations = keyObtentionIterations;
        this.saltSizeBytes = detectSaltSize(algorithm, provider);
    }

    /**
     * Convenience constructor using Rundeck's production defaults.
     */
    public static LegacyJasyptDecryptor defaultStorage() {
        return new LegacyJasyptDecryptor("PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000);
    }

    /**
     * Convenience constructor for the encrypt-datasource-password plugin defaults.
     */
    public static LegacyJasyptDecryptor datasourcePassword() {
        return new LegacyJasyptDecryptor("PBEWITHSHA256AND256BITAES-CBC-BC", "BC", 1000);
    }

    /**
     * Convenience constructor for the encryptable-core-properties plugin defaults.
     */
    public static LegacyJasyptDecryptor coreProperties() {
        return new LegacyJasyptDecryptor("PBEWithMD5AndDES", "BC", 1000);
    }

    /**
     * Decrypt data encrypted by Jasypt's {@code StandardPBEByteEncryptor}.
     *
     * @param password         the encryption password
     * @param encryptedMessage the Jasypt-produced bytes: {@code [salt][ciphertext]}
     * @return decrypted plaintext
     * @throws EncryptionException if decryption fails
     */
    public byte[] decrypt(char[] password, byte[] encryptedMessage) {
        if (encryptedMessage == null || encryptedMessage.length <= saltSizeBytes) {
            throw new EncryptionException("Encrypted message is null or too short for salt extraction");
        }

        try {
            byte[] salt = Arrays.copyOfRange(encryptedMessage, 0, saltSizeBytes);
            byte[] ciphertext = Arrays.copyOfRange(encryptedMessage, saltSizeBytes, encryptedMessage.length);

            SecretKeyFactory factory = provider != null
                    ? SecretKeyFactory.getInstance(algorithm, provider)
                    : SecretKeyFactory.getInstance(algorithm);
            SecretKey key = factory.generateSecret(new PBEKeySpec(password));

            Cipher cipher = provider != null
                    ? Cipher.getInstance(algorithm, provider)
                    : Cipher.getInstance(algorithm);

            PBEParameterSpec paramSpec = new PBEParameterSpec(salt, keyObtentionIterations);
            cipher.init(Cipher.DECRYPT_MODE, key, paramSpec);

            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new EncryptionException("Legacy Jasypt decryption failed", e);
        }
    }

    /**
     * Decrypt using a String password (convenience wrapper).
     */
    public byte[] decrypt(String password, byte[] encryptedMessage) {
        return decrypt(password.toCharArray(), encryptedMessage);
    }

    /**
     * @return the salt size in bytes for this decryptor's algorithm
     */
    public int getSaltSizeBytes() {
        return saltSizeBytes;
    }

    /**
     * Detect salt size by obtaining the cipher's block size, matching
     * Jasypt's {@code StandardPBEByteEncryptor.initialize()} behavior.
     */
    private static int detectSaltSize(String algorithm, String providerName) {
        try {
            Cipher cipher = providerName != null
                    ? Cipher.getInstance(algorithm, providerName)
                    : Cipher.getInstance(algorithm);
            int blockSize = cipher.getBlockSize();
            return blockSize > 0 ? blockSize : 8;
        } catch (Exception e) {
            throw new EncryptionException("Cannot determine salt size for algorithm: " + algorithm, e);
        }
    }
}
