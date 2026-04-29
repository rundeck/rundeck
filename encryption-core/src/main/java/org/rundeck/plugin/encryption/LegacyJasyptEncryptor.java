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
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
import java.security.SecureRandom;
import java.security.Security;

/**
 * Encrypts data in the same binary format as Jasypt's {@code StandardPBEByteEncryptor}
 * without requiring the Jasypt library.
 *
 * <p>Output format matches Jasypt exactly:
 * <pre>
 *   [salt : blockSize bytes] [ciphertext]
 * </pre>
 *
 * <p>This is the encrypt counterpart to {@link LegacyJasyptDecryptor}. It exists
 * primarily for backward compatibility testing and for scenarios where PBE format
 * output is still needed (e.g., round-trip verification against real Jasypt).
 *
 * @see LegacyJasyptDecryptor
 */
public class LegacyJasyptEncryptor {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private final String algorithm;
    private final String provider;
    private final int keyObtentionIterations;
    private final int saltSizeBytes;
    private final SecureRandom random;

    /**
     * @param algorithm              PBE algorithm name (e.g. {@code PBEWITHSHA256AND128BITAES-CBC-BC})
     * @param provider               JCE provider name (e.g. {@code "BC"}) or {@code null} for default
     * @param keyObtentionIterations number of PBE hashing iterations (Jasypt default: 1000)
     */
    public LegacyJasyptEncryptor(String algorithm, String provider, int keyObtentionIterations) {
        this.algorithm = algorithm;
        this.provider = provider;
        this.keyObtentionIterations = keyObtentionIterations;
        this.saltSizeBytes = detectSaltSize(algorithm, provider);
        this.random = new SecureRandom();
    }

    /** Convenience constructor using Rundeck's production defaults (storage). */
    public static LegacyJasyptEncryptor defaultStorage() {
        return new LegacyJasyptEncryptor("PBEWITHSHA256AND128BITAES-CBC-BC", "BC", 1000);
    }

    /** Convenience constructor for encrypt-datasource-password defaults. */
    public static LegacyJasyptEncryptor datasourcePassword() {
        return new LegacyJasyptEncryptor("PBEWITHSHA256AND256BITAES-CBC-BC", "BC", 1000);
    }

    /** Convenience constructor for encryptable-core-properties defaults. */
    public static LegacyJasyptEncryptor coreProperties() {
        return new LegacyJasyptEncryptor("PBEWithMD5AndDES", "BC", 1000);
    }

    /**
     * Encrypt data in Jasypt-compatible PBE format: {@code [salt][ciphertext]}.
     *
     * @param password  the encryption password
     * @param plaintext data to encrypt
     * @return encrypted bytes: {@code [salt][ciphertext]}
     * @throws EncryptionException if encryption fails
     */
    public byte[] encrypt(char[] password, byte[] plaintext) {
        if (plaintext == null) {
            throw new EncryptionException("Plaintext cannot be null");
        }
        try {
            byte[] salt = new byte[saltSizeBytes];
            random.nextBytes(salt);

            SecretKeyFactory factory = provider != null
                    ? SecretKeyFactory.getInstance(algorithm, provider)
                    : SecretKeyFactory.getInstance(algorithm);
            SecretKey key = factory.generateSecret(new PBEKeySpec(password));

            Cipher cipher = provider != null
                    ? Cipher.getInstance(algorithm, provider)
                    : Cipher.getInstance(algorithm);

            PBEParameterSpec paramSpec = new PBEParameterSpec(salt, keyObtentionIterations);
            cipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] result = new byte[salt.length + ciphertext.length];
            System.arraycopy(salt, 0, result, 0, salt.length);
            System.arraycopy(ciphertext, 0, result, salt.length, ciphertext.length);
            return result;
        } catch (Exception e) {
            throw new EncryptionException("Legacy PBE encryption failed", e);
        }
    }

    /**
     * Encrypt using a String password (convenience wrapper).
     */
    public byte[] encrypt(String password, byte[] plaintext) {
        return encrypt(password.toCharArray(), plaintext);
    }

    /**
     * @return the salt size in bytes for this encryptor's algorithm
     */
    public int getSaltSizeBytes() {
        return saltSizeBytes;
    }

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
