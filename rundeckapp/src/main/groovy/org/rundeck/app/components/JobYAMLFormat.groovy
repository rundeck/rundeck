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

package org.rundeck.app.components

import com.dtolabs.rundeck.app.support.BuilderUtil
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import org.rundeck.app.components.jobs.JobDefinitionException
import org.rundeck.app.components.jobs.JobFormat
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

/**
 * Definition for jobs YAML format
 */
@CompileStatic
class JobYAMLFormat implements JobFormat {
    final String format = 'yaml'

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    List<Map> decode(final Reader reader) throws JobDefinitionException {
        Yaml yaml = new Yaml(new SafeConstructor())
        def data = yaml.load(reader)
        if (!(data instanceof List)) {
            throw new JobDefinitionException("Yaml: Expected list data")
        }
        if (!data.every { it instanceof Map }) {
            throw new JobDefinitionException("Yaml: Expected list of Maps")
        }
        return data
    }

    static Object canonicalValue(Object val) {
        if (val instanceof Map) {
            return canonicalMap(val)
        } else if (val instanceof String) {
            //set multiline strings to use unix line endings
            return BuilderUtil.replaceLineEndings(val, DumperOptions.LineBreak.UNIX.getString())
        } else if (val instanceof List) {
            return val.collect(JobYAMLFormat.&canonicalValue)
        }
        return val
    }

    static Map canonicalMap(Map input) {
        def result = [:]//linked hash map has ordered keys
        input.keySet().sort().each {
            def val = input[it]
            result[it] = canonicalValue(val)
        }
        result
    }

    @Override
    void encode(final List<Map> list, Options options, final Writer writer) {
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.lineBreak = DumperOptions.LineBreak.UNIX
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions)
        def mapping = { Map<String, Object> map ->
            if (options.replaceIds && options.replaceIds.get(map['id'])) {
                map['id'] = options.replaceIds.get(map.remove('id'))
                map['uuid'] = options.replaceIds.get(map.remove('uuid'))
            } else if (!options.preserveUuid) {
                map.remove('id')
                map.remove('uuid')
            }
            map
        }

        yaml.dump(list.collect { canonicalMap(mapping(it)) }, writer)
    }
}
