/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

package rundeck.codecs

import com.dtolabs.rundeck.app.support.BuilderUtil
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.constructor.SafeConstructor
import rundeck.ScheduleDef
import rundeck.controllers.ScheduleDefYAMLException

class ScheduleDefinitionYAMLCodec {

    static Object canonicalValue(Object val) {
        if(val instanceof Map){
            return canonicalMap(val)
        }else if (val instanceof String){
            //set multiline strings to use unix line endings
            return BuilderUtil.replaceLineEndings(val,DumperOptions.LineBreak.UNIX.getString())
        }else if(val instanceof List) {
            return val.collect(ScheduleDefinitionYAMLCodec.&canonicalValue)
        }
        return val
    }
    static Map canonicalMap(Map input) {
        def result = [:]//linked hash map has ordered keys
        input.keySet().sort().each{
            def val = input[it]

            result[it]=canonicalValue(val)
        }
        result
    }

    static encodeStripUuid(List list) {
        encodeReplaceUuid(list, [:])
    }

    static encodeReplaceUuid(List list, Map replaceIds) {
        def writer = new StringWriter()
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.lineBreak = DumperOptions.LineBreak.UNIX
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions)
        def mapping = {
            def map = it.toMap()
            if (replaceIds && replaceIds[map['id']]) {
                map['id'] = replaceIds[map.remove('id')]
                map['uuid'] = replaceIds[map.remove('uuid')]
            } else {
                map.remove('id')
                map.remove('uuid')
            }
            map
        }

        yaml.dump(list.collect { canonicalMap(mapping(it)) }, writer)

        return writer.toString()
    }

    static encodeMaps(List list, boolean preserveUuid = true, Map<String, String> replaceIds = [:]) {
        def writer = new StringWriter()
        final DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.lineBreak = DumperOptions.LineBreak.UNIX
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Yaml yaml = new Yaml(dumperOptions)
        def mapping = {map->
            if (replaceIds && replaceIds[map['id']]) {
                map['id'] = replaceIds[map.remove('id')]
                map['uuid'] = replaceIds[map.remove('uuid')]
            } else if (!preserveUuid) {
                map.remove('id')
                map.remove('uuid')
            }
            map
        }

        yaml.dump(list.collect{canonicalMap(mapping(it))}, writer)

        return writer.toString()
    }
    static encode = { list ->

        return encodeMaps(list.collect { it.toMap() })
    }

    static decodeFromStream = {InputStream stream ->

        Yaml yaml = new Yaml(new SafeConstructor())

        def data = yaml.load(stream)
        return createScheduleDefinitions(data);
    }

    static decode = {input ->
        Yaml yaml = new Yaml(new SafeConstructor())
        def data
        if(input instanceof File){
            input = input.getInputStream()
        }
        data= yaml.load(input)
        return createScheduleDefinitions(data);
    }

    public static createScheduleDefinitions(data){
        ArrayList list = new ArrayList()
        if (data instanceof Collection) {
            data.each{ scheduleDefMap ->
                if (scheduleDefMap instanceof Map) {
                    try {
                        list << ScheduleDef.fromMap(scheduleDefMap)
                    } catch (Exception e) {
                        throw new ScheduleDefYAMLException("Unable to create Schedule Definition: " + e.getMessage(),e)
                    }
                } else {
                    throw new ScheduleDefYAMLException("Unexpected data type: " + scheduleDefMap.class.name)
                }
            }
        } else if (data instanceof Map) {
            data.scheduleDefinitions.each { scheduleDefMap ->
                if (scheduleDefMap instanceof Map) {
                    try {
                        list << ScheduleDef.fromMap(scheduleDefMap)
                    } catch (Exception e) {
                        throw new ScheduleDefYAMLException("Unable to create Schedule Definition: " + e.getMessage(), e)
                    }
                } else {
                    throw new ScheduleDefYAMLException("Unexpected data type: " + scheduleDefMap.class.name)
                }
            }
        } else {
            throw new ScheduleDefYAMLException("Unexpected data type: " + data.class.name)
        }
        return list
    }

}
