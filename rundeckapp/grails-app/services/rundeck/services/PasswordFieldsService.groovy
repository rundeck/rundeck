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


import com.dtolabs.rundeck.core.plugins.PluginConfiguration
import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.security.InvalidKeyException
import java.security.SecureRandom

class PasswordFieldsService {

    static scope = "session"

    private final Map<Fieldkey, Map<String, String>> fields = Collections.synchronizedMap(new HashMap<Fieldkey, Map<String, String>>())
    private final byte[] sessionPassphrase = generateSessionPassphrase()

    private static byte[] generateSessionPassphrase() {
        SecureRandom random = new SecureRandom()
        byte[] bytes = new byte[32]
        random.nextBytes(bytes)
        bytes
    }

    public reset() {
        fields.clear();
    }

    public reset(String arg) {
        synchronized (fields) {
            def keys = fields.keySet().findAll { it.arg == arg }
            keys.each { fields.remove(it) }
        }
    }

    public int tracking() {
        return fields.size()
    }

    public int tracking(String arg) {
        synchronized (fields) {
            def keys = fields.keySet().findAll { it.arg == arg }
            return keys.size()
        }
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
        synchronized (fields) {
            fields.keySet().findAll {
                it.position == pos
            }.each {
                fields.remove(it)
            }
        }
    }

    /**
     * Track password values by replacing them with hashed versions
     * @param configurations Collection of Map, Map should have entries "type" (plugin type) and "props" (Map of key/values)
     * @param descriptions list of possible plugins
     * @return
     */
    public int track(Collection configurations, Collection<Description> descriptions) {
        track('_', configurations, descriptions)
    }
    /**
     * Reset the arg, and Track password values by replacing them with hashed versions
     * @param configurations Collection of Map, Map should have entries "type" (plugin type) and "props" (Map of
     * key/values)
     * @param descriptions list of possible plugins
     * @return
     */
    public int resetTrack(String arg, Collection configurations, Collection<Description> descriptions) {
        synchronized (fields) {
            reset(arg)
            return track(arg, configurations, descriptions)
        }
    }
    /**
     * Reset the arg, and Track password values by replacing them with hashed versions
     * @param configurations Collection of Map, Map should have entries "type" (plugin type) and "props" (Map of
     * key/values)
     * @param hidden if true, use obscure string instead of hash
     * @param descriptions list of possible plugins
     * @return
     */
    public int resetTrack(String arg, Collection configurations, boolean hidden, Collection<Description> descriptions) {
        synchronized (fields) {
            reset(arg)
            return track(arg, configurations, hidden, descriptions)
        }
    }
    /**
     * Track password values by replacing them with hashed versions
     * @param configurations Collection of Map, Map should have entries "type" (plugin type) and "props" (Map of
     * key/values)
     * @param descriptions list of possible plugins
     * @return
     */
    public int track(String arg, Collection configurations, Collection<Description> descriptions) {
        return track(arg, configurations, false, descriptions)
    }

    public int track(configurations, boolean hidden, Collection<Description> descriptions) {
        track('_', configurations, hidden, descriptions)
    }

    public int track(String arg, Collection configurations, boolean hidden, Collection<Description> descriptions) {
        if(!configurations) {
            return 0
        }

        int count = 0
        int configPos = 0
        for (config in configurations) {
            if (config == null) {
                continue
            }

            Description desc = findDescription(descriptions, config)
            if (desc == null) {
                configPos++
                continue
            }

            for (property in desc.getProperties()) {
                if (isPasswordDisplay(property)) {
                    String key = property.getName()
                    String value = getConfigProp(config, key)
                    if(value!=null){
                        def h = hidden?'*****':hash(value)

                        setConfigProp(h, config, key)
                        fields.put(fieldKey(arg, key, configPos), [original: value, hash: h])
                        count++
                    }
                }
            }
            configPos++
        }
        return count
    }

    public void setConfigProp(String h, config, String key) {
        if (config instanceof PluginConfiguration) {
            if (h != null) {
                config.configuration[key] = h
            } else {
                config.configuration.remove(key)
            }
        } else {
            if (h != null) {
                config.props[key] = h
            } else {
                config.props.remove(key)
            }
        }

    }

    public Object getConfigProp(config, String key) {
        if (config instanceof PluginConfiguration) {
            config.configuration[key]
        } else {
            config.props[key]
        }
    }

    public Description findDescription(Collection<Description> descriptions, config) {
        if (config instanceof PluginConfiguration) {
            descriptions.find { it.name == config.provider }
        } else {
            descriptions.find { it.name == config.type }
        }
    }

    /**
     * generate a key object
     * @param key
     * @param configPos
     * @return
     */
    private static Fieldkey fieldKey(String arg, String key, int configPos) {
        [arg: arg, name: key, position: configPos] as Fieldkey
    }
    /**
     * Key class suitable for hash map
     */
    static class Fieldkey {
        String arg
        String name
        Integer position

        boolean equals(o) {
            if (this.is(o)) return true
            if (getClass() != o.class) return false

            Fieldkey keyvalue = (Fieldkey) o

            if (arg != keyvalue.arg) {
                return false
            }
            if (name != keyvalue.name) return false
            if (position != keyvalue.position) return false

            return true
        }

        int hashCode() {
            int result
            result = arg.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + position.hashCode()
            return result
        }
    }

    /**
     * Untrack hashed password values
     * @param configurations list of Maps.  Each map has entries: [ config: [type: String, props: Map], index: Integer]
     * @param descriptions plugin descriptions
     * @return
     */
    public int untrack(Collection configurations, Collection<Description> descriptions) {
        untrack('_', configurations, descriptions)
    }

    public int untrack(String arg, Collection configurations, Collection<Description> descriptions) {
        def count = 0
        for(resource in configurations) {
            if (resource == null) {
                continue
            }
            def config = resource.config
            if(config == null) {
                continue
            }

            if (null == resource.index) {
                continue
            }

            Integer configurationPosition = resource.index


            Description desc = findDescription(descriptions, resource)
            if (desc == null) {
                continue
            }

            for (property in desc.getProperties()) {
                if (!isPasswordDisplay(property)) {
                    continue
                }

                String key = property.getName()
                String value = getConfigProp(config, key)

                if (!fields.containsKey(fieldKey(arg, key, configurationPosition))) {

                    continue
                }


                def field = fields[fieldKey(arg, key, configurationPosition)]

                if (value != field.hash) {
                    if(value){
                        setConfigProp(value, config, key)
                    }else{
                        setConfigProp(null, config, key)
                    }
                } else {
                    setConfigProp(field.original, config, key)
                }

                fields.remove(fieldKey(arg, key, configurationPosition))
                count++

            }
        }
        count
    }

    private boolean isPasswordDisplay(Property property) {
        property.renderingOptions[StringRenderingConstants.DISPLAY_TYPE_KEY]?.toString() == StringRenderingConstants.DisplayType.PASSWORD.toString()
    }

    private String hash(String input) {
        return hmac_sha256(sessionPassphrase, input)
    }

    /**
     * @param secretKey
     * @param data
     * @return HMAC/SHA256 representation of the given string
     */
    private static String hmac_sha256(byte[] secretKey, String data) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "HmacSHA256")
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
