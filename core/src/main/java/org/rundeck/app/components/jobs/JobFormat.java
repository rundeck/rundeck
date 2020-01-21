/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.components.jobs;

import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Interface to define a Job definition format
 */
public interface JobFormat {
    /**
     * @return format name
     */
    String getFormat();

    /**
     * Decode input to canonical Job Map data
     *
     * @param reader
     * @return List of maps
     * @throws JobDefinitionException
     */
    List<Map> decode(Reader reader) throws JobDefinitionException;

    /**
     * Encode list of canonical Job Maps
     *
     * @param list    list of maps
     * @param options encode options
     * @param writer  writer
     */
    void encode(List<Map> list, Options options, Writer writer);

    /**
     * Create options object
     *
     * @param preserveUuid true to preserve UUIDs
     * @param replaceIds   replacement map for replacing UUIDs
     * @param stripJobRef  option for stripping Job Reference data
     * @return options
     */
    static Options options(boolean preserveUuid, Map<String, String> replaceIds, String stripJobRef) {
        return options(preserveUuid, replaceIds, null != stripJobRef ? StripJobRef.valueOf(stripJobRef) : null);
    }

    /**
     * Create options object
     *
     * @param preserveUuid true to preserve UUIDs
     * @param replaceIds   replacement map for replacing UUIDs
     * @param stripJobRef  option for stripping Job Reference data
     * @return options
     */
    static Options options(boolean preserveUuid, Map<String, String> replaceIds, StripJobRef stripJobRef) {
        return new Options() {
            @Override
            public boolean isPreserveUuid() {
                return preserveUuid;
            }

            @Override
            public Map<String, String> getReplaceIds() {
                return replaceIds;
            }

            @Override
            public StripJobRef getStripJobRef() {
                return stripJobRef;
            }
        };
    }

    /**
     * @return default options
     */
    static Options defaultOptions() {
        return options(true, new HashMap<>(), (StripJobRef) null);
    }

    /**
     * Encode options
     */
    interface Options {
        /**
         * @return true to preserve UUID in output
         */
        boolean isPreserveUuid();

        /**
         * @return map of replacement IDs
         */
        Map<String, String> getReplaceIds();

        /**
         * @return strip job ref option, or null
         */
        StripJobRef getStripJobRef();
    }

    /**
     * Options for stripping Job Reference data
     */
    enum StripJobRef {
        /**
         * Strip the UUID
         */
        uuid,
        /**
         * Strip the name/group
         */
        name
    }
}
