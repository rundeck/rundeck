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

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.security.Security;

/**
 * Encrypts and decrypts data using AES-256-GCM with PBKDF2 key derivation.
 *
 * <p>Wire format (version 1):
 * <pre>
 *   [version : 1 byte] [salt : 16 bytes] [iv : 12 bytes] [ciphertext + GCM auth tag (16 bytes)]
 * </pre>
 *
 * <ul>
 *   <li>Version byte allows future algorithm changes without breaking existing data.</li>
 *   <li>Random salt per encryption ensures unique derived keys.</li>
 *   <li>Random 12-byte IV is the NIST recommendation for GCM.</li>
 *   <li>GCM provides authenticated encryption — tampered data is rejected.</li>
 * </ul>
 */
public class ModernEncryptor {

    static {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    static final byte FORMAT_VERSION = 0x01;
    static final int SALT_SIZE = 16;
    static final int IV_SIZE = 12;
    static final int GCM_TAG_BITS = 128;
    static final int KEY_SIZE_BITS = 256;
    static final int PBKDF2_ITERATIONS = 100_000;
    static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";

    private static final int HEADER_SIZE = 1 + SALT_SIZE + IV_SIZE;

    private final SecureRandom random;

    public ModernEncryptor() {
        this.random = new SecureRandom();
    }

    /**
     * Encrypt plaintext using AES-256-GCM with a password-derived key.
     *
     * @param password  encryption password
     * @param plaintext data to encrypt
     * @return encrypted bytes in the format: {@code [version][salt][iv][ciphertext+tag]}
     * @throws EncryptionException if encryption fails
     */
    public byte[] encrypt(char[] password, byte[] plaintext) {
        if (plaintext == null) {
            throw new EncryptionException("Plaintext cannot be null");
        }
        try {
            byte[] salt = new byte[SALT_SIZE];
            random.nextBytes(salt);

            byte[] iv = new byte[IV_SIZE];
            random.nextBytes(iv);

            SecretKey key = deriveKey(password, salt);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, gcmSpec);
            byte[] ciphertext = cipher.doFinal(plaintext);

            ByteBuffer buffer = ByteBuffer.allocate(HEADER_SIZE + ciphertext.length);
            buffer.put(FORMAT_VERSION);
            buffer.put(salt);
            buffer.put(iv);
            buffer.put(ciphertext);
            return buffer.array();
        } catch (Exception e) {
            throw new EncryptionException("AES-256-GCM encryption failed", e);
        }
    }

    /**
     * Convenience wrapper accepting a String password.
     */
    public byte[] encrypt(String password, byte[] plaintext) {
        return encrypt(password.toCharArray(), plaintext);
    }

    /**
     * Decrypt data produced by {@link #encrypt}.
     *
     * @param password  the same password used for encryption
     * @param encrypted bytes in the format: {@code [version][salt][iv][ciphertext+tag]}
     * @return decrypted plaintext
     * @throws EncryptionException if decryption fails or data is tampered
     */
    public byte[] decrypt(char[] password, byte[] encrypted) {
        if (encrypted == null || encrypted.length <= HEADER_SIZE) {
            throw new EncryptionException("Encrypted data is null or too short");
        }

        ByteBuffer buffer = ByteBuffer.wrap(encrypted);

        byte version = buffer.get();
        if (version != FORMAT_VERSION) {
            throw new EncryptionException("Unsupported encryption format version: " + version);
        }

        byte[] salt = new byte[SALT_SIZE];
        buffer.get(salt);

        byte[] iv = new byte[IV_SIZE];
        buffer.get(iv);

        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        try {
            SecretKey key = deriveKey(password, salt);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, gcmSpec);
            return cipher.doFinal(ciphertext);
        } catch (AEADBadTagException e) {
            throw new EncryptionException(
                    "Decryption failed: authentication tag mismatch (wrong password or tampered data)", e);
        } catch (Exception e) {
            throw new EncryptionException("AES-256-GCM decryption failed", e);
        }
    }

    /**
     * Convenience wrapper accepting a String password.
     */
    public byte[] decrypt(String password, byte[] encrypted) {
        return decrypt(password.toCharArray(), encrypted);
    }

    /**
     * Check if the given bytes appear to be in modern encryption format.
     */
    public static boolean isModernFormat(byte[] data) {
        return data != null && data.length > HEADER_SIZE && data[0] == FORMAT_VERSION;
    }

    private SecretKey deriveKey(char[] password, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password, salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
            SecretKey tmp = factory.generateSecret(spec);
            return new SecretKeySpec(tmp.getEncoded(), "AES");
        } finally {
            spec.clearPassword();
        }
    }
}
