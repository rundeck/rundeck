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

/**
 * Decompact a list of maps in compacted form, and call func for each entry after decompaction
 * @param entries array of entries. The first item must be an object, and all others must be an object or a string.
 *     Each decompacted entry is an object. The decompacted form of the first entry is the same as the first entry. The
 *     decompacted form of each subsequent entry is created by duplicating all missing object keys from the previous
 *     decompacted entry into the new one.  If the new entry has 'null' values for a key, that key is removed.  If the
 *     new entry is a simple string, the new entry is replaced with an object with only the "compactedAttr" key, whose
 *     value is the entry string, before decompaction. If a key is '_' it is replaced with the compactedAttr key.
 * @param compactedAttr an object key that will be used for simple string decompaction
 * @param func called for each decompacted entry
 * @private
 */
function _decompactMapList(entries, compactedAttr, func) {
    var odata = {};
    for (var i = 0; i < entries.length; i++) {
        var e = entries[i];
        //fill data from previous entry
        //compactedAttr is attr name to replace '_' with
        if (compactedAttr && typeof(e) === 'string') {
            var s = e;
            e = {};
            e[compactedAttr] = s;
        } else if (compactedAttr && typeof(e['_']) !== 'undefined') {
            e[compactedAttr] = e['_'];
            delete e['_'];
        }
        for (k in odata) {
            if (odata.hasOwnProperty(k)) {
                if (typeof(e[k]) === 'undefined') {
                    e[k] = odata[k]
                } else if (e[k] === null) {
                    delete e[k];
                }
            }
        }
        odata = e;
        func(e);
    }
}