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

package rundeck.controllers

class ParamsUtil {
    /**
     * Remove map entries where the value is null or a blank string
     * @param map
     * @return
     */
    static Object cleanMap(Map map) {
        def datamap = map ? map.entrySet().
            findAll { it.value && !it.key.startsWith('_') }.
            collectEntries { [it.key, it.value] } : [:]
        datamap = decomposeMap(datamap)
        parseDataMap(datamap)
    }
    /**
     * Remove map entries where the value is null or a blank string
     * @param map
     * @return
     */
    static Object parseDataMap(Map datamap) {
        //parse map type entries
        def reslist = parseTypedMap(datamap)
        if (reslist instanceof Collection) {
            return reslist.collect {
                parseDataMap(it)
            }
        } else if (reslist instanceof Map) {
            def result = [:]
            reslist.each { k, v ->
                if (v instanceof Map) {
                    result[k] = parseDataMap(v)
                } else {
                    result[k] = v
                }
            }
            return result
        }
        reslist
    }
    public static def parseTypedMap(Map datamap) {
        def type = datamap['_type']
        if (type == 'list') {
            return parseListTypeEntries(datamap)
        } else if (type == 'map') {
            return parseMapTypeEntries(datamap)
        } else if (type == 'embedded') {
            return parseEmbeddedTypeEntries(datamap)
        } else if (type == 'embeddedPlugin') {
            return parseEmbeddedPluginEntries(datamap)
        }
        datamap
    }
    /**
     * Finds all "map" type entries, and converts them using the {@link #parseIndexedMapParams(java.util.Map)}.
     * A map entry is defined as an entry PREFIX, where a key PREFIX_.type is present with value "map",
     * and a PREFIX.map is present which can be parsed as an indexed map param. (Alternately, if the PREFIX value is a Map which contains a "map" entry, that is used.)
     * All entries starting with "PREFIX." are
     * removed and an entry PREFIX is created which contains the parsed indexed map.
     * @param datamap
     */
    public static Map parseMapTypeEntries(Map datamap) {
        parseMapEntries(datamap, 'map', 'map', true, null)
    }
    /**
     * Finds all "embedded" type entries
     * @param datamap
     */
    public static Map parseEmbeddedTypeEntries(Map datamap) {
        parseMapEntries(datamap, 'embedded', 'config', false, null)
    }
    /**
     * Finds all "embeddedPlugin" type entries, and expects it to contains a set of 'config' entries (config map values),
     * and a 'type' entry (plugin provider type)
     * @param datamap
     */
    public static Map parseEmbeddedPluginEntries(Map datamap) {
        parseMapEntries(datamap, 'embeddedPlugin', 'config', false) { data, map ->
            def type = data['type']
            type ? [type: type, config: map] : map
        }
    }
    /**
     * Finds all "list" type entries, and returns a list with each entry parsed, otherwise returns the original map
     * @param datamap
     */
    public static Object parseListTypeEntries(Map datamap) {
        parseMapEntries(datamap, 'list', null, false) { data, map ->
            parseMapList(map)
        }
    }
    public static Object parseMapEntries(
        Map datamap,
        String typeVal,
        String suffix,
        Boolean expectIndexed,
        Closure transform
    ) {
        def thetype = datamap.get('_type')
        Object mapval = suffix ? datamap.get(suffix) : datamap
        def result = [:]
        if (thetype == typeVal && (mapval instanceof Map)) {
            def pmap = expectIndexed ? parseIndexedMapParams(mapval) : mapval
            result = transform ? transform(datamap, pmap) : pmap
        }
        return result
    }
    public static Object getMapValForKey(String keyname, String sufval, Map datamap) {
        String suffix
        def mapval = datamap.get(keyname + sufval)
        if (!mapval && datamap.get(keyname) instanceof Map && suffix) {
            //nb: if the input is JSONObject, a .get() to non-existent key will throw exception
            mapval = datamap.get(keyname).containsKey(suffix) ? datamap.get(keyname).get(suffix) : null
        }
        if (!mapval) {
            //collect prefixes
            def testprefix = keyname + sufval + '.'
            def list = datamap.keySet().findAll { it.startsWith(testprefix) }
            if (list) {
                mapval = list.collectEntries { String k ->
                    def val = datamap[k]
                    [k.substring(testprefix.length()), val]
                }
            }
        }
        mapval
    }
    /**
     *
     * @param data
     * @return
     */
    static List parseMapList(Map data) {
        def entries = data?[data.get("_indexes")].flatten():[]
        List result = []
        entries.each { index ->
            def map = getMapOrPrefixedMap(data, "entry[${index}]".toString())
            if (map) {
                result << parseDataMap(map)
            }
        }
        result
    }
    public static Map getMapOrPrefixedMap(Map data, String prefix) {
        def res = data.get(prefix)
        if (res && res instanceof Map) {
            return res
        }
        //build map from all entries with the prefix
        def testprefix = prefix + '.'
        def list = data.keySet().findAll { it.startsWith(testprefix) }
        if (list) {
            return list.collectEntries { String k ->
                def val = data[k]
                [k.substring(testprefix.length()), val]
            }
        }
        null
    }
    /**
     * Parse input data with index key/value entries into a map.
     * If a key of the form "0.key" exists,  look for 0.value, to find a key/value pair, then
     * increment the index until no corresponding key is found. Empty keys are skipped, but
     * considered valid indexes. Empty or null values are interpreted as empty strings.
     *
     * @param map
     * @return the key/value data as a single map, or empty map if none is found
     */
    static Map parseIndexedMapParams(Map map) {
        int index = 0
        def data = [:]
        if (!map) {
            return data;
        }
        while (map.containsKey("${index}.key".toString()) || map.containsKey("${index}".toString())) {
            def val = map["${index}"]
            if (val instanceof Map && val.containsKey('key')) {
                def key = val['key']
                def value = val['value']
                if (key) {
                    data[key] = value?.toString() ?: ''
                }
            } else {
                def key = map["${index}.key".toString()]
                def value = map["${index}.value".toString()]
                if (key) {
                    data[key] = value?.toString() ?: ''
                }
            }
            index++
        }
        data
    }
    /**
     * decompose . separated keys into submaps
     * @param map
     * @return
     */
    static Map<String, String> decomposeMap(final Map<String, ?> map) {
        Map<String, String> result = [:]
        map.keySet().each { key ->
            def value = map[key]
            def list = key.split(/\./)
            def cur = result
            def last = key
            for (def i = 0; i < list.length - 1; i++) {
                def sub = list[i]
                if (null == cur[sub]) {
                    cur[sub] = [:]
                }
                if (!(cur[sub] instanceof Map)) {
                    //instead prefix the next entry with current path item
                    def remove = cur.remove(sub)
                    cur[sub] = [_value: remove]
                }
                cur = cur[sub]
            }
            last = list[list.length - 1]
            if (null != cur && null != last) {
                if (!cur[last]) {
                    cur[last] = value
                } else if (cur[last] instanceof Map) {
                    cur[last]['_value'] = value
                } else if (cur[last] instanceof List) {
                    cur[last] << value
                } else if (cur[last]) {
                    def val=cur[last]
                    cur[last] = [val, value]
                }
            }
        }
        result
    }
}