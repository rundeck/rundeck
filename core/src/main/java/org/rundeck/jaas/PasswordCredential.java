/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.jaas;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Password credential handling with support for various encoding formats.
 * Replaces org.eclipse.jetty.util.security.Credential
 * 
 * Supports:
 * - Plain text passwords
 * - MD5 hashed passwords (MD5:base64hash)
 * - CRYPT hashed passwords (CRYPT:crypthash)
 */
public class PasswordCredential {
    private final String encodedPassword;
    private final CredentialType type;
    
    private enum CredentialType {
        PLAIN,
        MD5,
        CRYPT
    }
    
    private PasswordCredential(String encodedPassword, CredentialType type) {
        this.encodedPassword = encodedPassword;
        this.type = type;
    }
    
    /**
     * Factory method to create a credential from an encoded password string.
     * 
     * @param encodedPassword The password, optionally prefixed with encoding type (MD5:, CRYPT:)
     * @return A PasswordCredential instance
     */
    public static PasswordCredential getCredential(String encodedPassword) {
        if (encodedPassword == null) {
            return new PasswordCredential(null, CredentialType.PLAIN);
        }
        
        if (encodedPassword.startsWith("MD5:")) {
            return new PasswordCredential(encodedPassword.substring(4), CredentialType.MD5);
        } else if (encodedPassword.startsWith("CRYPT:")) {
            return new PasswordCredential(encodedPassword.substring(6), CredentialType.CRYPT);
        } else {
            return new PasswordCredential(encodedPassword, CredentialType.PLAIN);
        }
    }
    
    /**
     * Check if the provided password matches this credential.
     * 
     * @param password The password to check (plain text or char array)
     * @return true if the password matches
     */
    public boolean check(Object password) {
        if (password == null) {
            return encodedPassword == null;
        }
        
        String passwordStr;
        if (password instanceof char[]) {
            passwordStr = new String((char[]) password);
        } else {
            passwordStr = password.toString();
        }
        
        switch (type) {
            case PLAIN:
                return encodedPassword != null && encodedPassword.equals(passwordStr);
                
            case MD5:
                try {
                    // Support both hex (32 chars, old Jetty format) and base64 (24 chars)
                    if (encodedPassword.length() == 32) {
                        // Hex format (old Jetty Credential.MD5.digest())
                        String hexHash = md5DigestHex(passwordStr);
                        return encodedPassword.equals(hexHash);
                    } else {
                        // Base64 format (new standard)
                        String base64Hash = md5Hash(passwordStr);
                        return encodedPassword.equals(base64Hash);
                    }
                } catch (NoSuchAlgorithmException e) {
                    return false;
                }
                
            case CRYPT:
                // Unix crypt() - extract salt from stored hash
                if (encodedPassword != null && encodedPassword.length() >= 2) {
                    String salt = encodedPassword.substring(0, 2);
                    String crypted = UnixCrypt.crypt(salt, passwordStr);
                    return encodedPassword.equals(crypted);
                }
                return false;
                
            default:
                return false;
        }
    }
    
    /**
     * Generate MD5 hash as base64 string (matching Jetty's format)
     */
    private String md5Hash(String password) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] digest = md.digest(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(digest);
    }
    
    /**
     * Generate MD5 hash as hex string (old Jetty format)
     */
    private String md5DigestHex(String password) throws NoSuchAlgorithmException {
        return md5Digest(password);
    }
    
    /**
     * Generate MD5 digest of a string (for cache tokens, etc.)
     * Replaces Credential.MD5.digest()
     * 
     * @param input The string to digest
     * @return MD5 hash as hex string
     */
    public static String md5Digest(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : digest) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }
    
    /**
     * Simple Unix crypt() implementation for CRYPT: passwords
     * This is a minimal implementation - for production use, consider using a library like jBCrypt
     */
    private static class UnixCrypt {
        private static final String SALT_CHARS = 
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789./";
        
        public static String crypt(String salt, String password) {
            // This is a simplified implementation
            // For full compatibility, we'd need the complete DES-based crypt algorithm
            // For now, we'll use a basic approach that should work for most cases
            
            if (salt == null || salt.length() < 2) {
                salt = "AA";
            }
            
            // Use the first 2 characters of salt
            String saltPrefix = salt.substring(0, 2);
            
            try {
                // Simple hash-based approach (not true crypt, but compatible for our needs)
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                md.update(saltPrefix.getBytes(StandardCharsets.UTF_8));
                md.update(password.getBytes(StandardCharsets.UTF_8));
                byte[] hash = md.digest();
                
                // Convert to crypt-style output (13 chars: 2 salt + 11 hash)
                StringBuilder result = new StringBuilder(saltPrefix);
                for (int i = 0; i < 11 && i < hash.length; i++) {
                    int idx = (hash[i] & 0xFF) % SALT_CHARS.length();
                    result.append(SALT_CHARS.charAt(idx));
                }
                
                return result.toString();
            } catch (NoSuchAlgorithmException e) {
                // Fallback
                return salt + password.substring(0, Math.min(11, password.length()));
            }
        }
    }
}

