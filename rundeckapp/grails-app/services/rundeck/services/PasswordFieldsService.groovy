/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package rundeck.services

import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.InvalidKeyException
import java.security.SecureRandom

class PasswordFieldsService {

    static scope = "session"

    private Map<String, String> fields = new HashMap<String, String>()
    private final sessionPassphrase = generateSessionPassphrase()

    private static String generateSessionPassphrase() {
        SecureRandom random = new SecureRandom()
        byte[] bytes = new byte[20]
        random.nextBytes(bytes)
        return byteArrayToString(bytes)
    }

    public reset() {
        fields.clear();
    }

    public int tracking() {
        return fields.size()
    }

    public int adjust(List<Integer> updates) {
        if (tracking() == 0) {
            return 0
        }
        int removed = 0
        int max = Collections.max(updates).intValue()
        1.step(max, 1) {
            if (!updates.contains(it)) {
                shift(it-1) // we track position zero based.
                removed++
            }
        }
        removed
    }

    private void shift(int pos) {
        fields.keySet().findAll {
            it.endsWith(Integer.toString(pos))
        }.each {
            fields.remove(it)
        }
    }

    /**
     * Track password values by replacing them with hashed versions
     * @param configurations Collection of Map, Map should have entries "type" (plugin type) and "props" (Map of key/values)
     * @param descriptions list of possible plugins
     * @return
     */
    public int track(configurations, Description... descriptions) {
        if(!configurations) {
            return 0
        }

        int count = 0
        int configPos = 0
        for (config in configurations) {
            if (config == null) {
                continue
            }

            Description desc = descriptions.find { it.name == config.type }
            if (desc == null) {
                configPos++
                continue
            }

            for (property in desc.getProperties()) {
                if (isPasswordDisplay(property)) {
                    String key = property.getName()
                    String value = config.props[key]
                    def h = hash(value)

                    config.props[key] = h
                    fields.put(key + configPos, [original: value, hash: h])
                    count++
                }
            }
            configPos++
        }
        return count
    }

    /**
     * Untrack hashed password values
     * @param configurations list of Maps.  Each map has entries: [ config: [type: String, props: Map], index: Integer]
     * @param descriptions plugin descriptions
     * @return
     */
    public int untrack(configurations, Description... descriptions) {
        def count = 0
        for(resource in configurations) {
            if (resource == null) {
                continue
            }
            Map config=resource.config
            if(config == null) {
                continue
            }
            Integer configurationPosition = resource.index



            Description desc = descriptions.find { it.name == config.type }
            if (desc == null) {
                continue
            }

            for (property in desc.getProperties()) {
                if (!isPasswordDisplay(property)) {
                    continue
                }

                String key = property.getName()
                String value = config.props[key]

                if (!fields.containsKey(key + configurationPosition)) {
                    config.props[key] = value
                    continue
                }


                def field = fields[key + configurationPosition]

                if (value != field.hash) {
                    config.props[key] = value
                } else {
                    config.props[key] = field.original
                }

                fields.remove(key + configurationPosition)
                count++

            }
        }
        count
    }

    private boolean isPasswordDisplay(Property property) {
        property.renderingOptions[StringRenderingConstants.DISPLAY_TYPE_KEY] == StringRenderingConstants.DisplayType.PASSWORD
    }

    private String hash(String input) {
        return hmac_sha256(sessionPassphrase, input)
    }

    /**
     * @param secretKey
     * @param data
     * @return HMAC/SHA256 representation of the given string
     */
    private static String hmac_sha256(String secretKey, String data) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes("UTF-8"), "HmacSHA256")
            Mac mac = Mac.getInstance("HmacSHA256")
            mac.init(secretKeySpec)
            byte[] digest = mac.doFinal(data.getBytes("UTF-8"))
            return byteArrayToString(digest)
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Invalid key exception while converting to HMac SHA256")
        }
    }

    private static String byteArrayToString(byte[] data) {
        BigInteger bigInteger = new BigInteger(1, data)
        String hash = bigInteger.toString(16)
        //Zero pad it
        while (hash.length() < 64) {
            hash = "0" + hash
        }
        return hash
    }

}
