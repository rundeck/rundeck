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

import org.apache.commons.codec.digest.UnixCrypt;
import org.eclipse.jetty.util.security.Password;
import org.springframework.security.crypto.bcrypt.BCrypt;

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
 * - MD5 hashed passwords (MD5:base64hash or MD5:hexhash)
 * - CRYPT hashed passwords (CRYPT:crypthash) - standard Unix DES crypt
 * - BCRYPT hashed passwords (BCRYPT:$2a$... or BCRYPT:$2b$...)
 * - OBF obfuscated passwords (OBF:...) - Jetty reversible obfuscation
 */
public class PasswordCredential {
    private final String encodedPassword;
    private final CredentialType type;
    
    private enum CredentialType {
        PLAIN,
        MD5,
        CRYPT,
        BCRYPT,
        OBF
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
        } else if (encodedPassword.startsWith("BCRYPT:")) {
            return new PasswordCredential(encodedPassword.substring(7), CredentialType.BCRYPT);
        } else if (encodedPassword.startsWith("OBF:")) {
            return new PasswordCredential(encodedPassword, CredentialType.OBF);
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
                if (encodedPassword != null && encodedPassword.length() >= 2) {
                    String crypted = UnixCrypt.crypt(passwordStr, encodedPassword);
                    return encodedPassword.equals(crypted);
                }
                return false;
                
            case BCRYPT:
                if (encodedPassword != null) {
                    try {
                        return BCrypt.checkpw(passwordStr, encodedPassword);
                    } catch (Exception e) {
                        return false;
                    }
                }
                return false;
                
            case OBF:
                if (encodedPassword != null) {
                    try {
                        String deobfuscated = Password.deobfuscate(encodedPassword);
                        return passwordStr.equals(deobfuscated);
                    } catch (Exception e) {
                        return false;
                    }
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
    
}

