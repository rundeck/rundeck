/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.core.plugins;

import org.apache.commons.codec.digest.DigestUtils;

import java.security.MessageDigest;

public class PluginUtils {
    private static String[] HEX = {"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
    public static String generateShaIdFromName(String pluginName) {
        MessageDigest digest = DigestUtils.getSha256Digest();
        digest.update(pluginName.getBytes());
        return bytesAsHex(digest.digest()).substring(0,12);
    }

    public static String bytesAsHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        StringBuilder result = new StringBuilder();

        for(byte b : bytes) {
            result.append(HEX[(b & 0xF0) >> 4]);
            result.append(HEX[b & 0x0F]);
        }
        return result.toString();
    }
}
