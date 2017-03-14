/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.util

/**
 * @author greg
 * @since 2/23/17
 */
class Sizes {
    static final Map<String, Long> UNITS = [
            t: 1024L * 1024 * 1024 * 1024,
            g: 1024 * 1024 * 1024,
            m: 1024 * 1024,
            k: 1024,
            b: 1
    ]

    /**
     * Parse a file size in the form "###[tgkm][b]" (case insensitive)
     * @param size
     * @return
     */
    static Long parseFileSize(String size) {
        def m = size =~ /(\d+)((?i)[tgmk]?b?)?/
        if (m.matches()) {
            def count = m.group(1)
            def unit = m.group(2)
            def multi = unit ? UNITS[unit[0]?.toLowerCase()] ?: 1 : 1
            def value = 0
            try {
                return Long.parseLong(count) * multi
            } catch (NumberFormatException e) {
                return null
            }
        }
        null
    }
}
