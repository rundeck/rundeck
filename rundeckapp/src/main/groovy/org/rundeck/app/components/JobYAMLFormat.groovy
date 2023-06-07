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
import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Represent
import org.yaml.snakeyaml.representer.Representer

/**
 * Definition for jobs YAML format
 */
@CompileStatic
class JobYAMLFormat implements JobFormat {
    final String format = 'yaml'

    Boolean trimSpacesFromLines = false

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    List<Map> decode(final Reader reader) throws JobDefinitionException {
        Yaml yaml = new Yaml(new LoaderOptions())
        def data = yaml.load(reader)
        if (!(data instanceof List)) {
            throw new JobDefinitionException("Yaml: Expected list data")
        }
        if (!data.every { it instanceof Map }) {
            throw new JobDefinitionException("Yaml: Expected list of Maps")
        }
        data.each (JobYAMLFormat.&convertJobNotifications)
        return data
    }
    static void convertJobNotifications(Map jobMap){
        Map notifTriggers = jobMap['notification'] as Map
        if(notifTriggers) {
            (notifTriggers as Map).keySet().each { trigger ->
                def notifsMap = notifTriggers[trigger]

                if (notifsMap instanceof Map) {
                    def notifList = []
                    Map notifToAdd = [:]
                    ((Map)notifsMap).each {
                        notifToAdd.put(it.key, it.value)
                    }
                    notifList.add(notifToAdd)
                    notifTriggers[trigger] = notifList
                }
            }
        }
    }

    static Object canonicalValue(Object val, boolean trimSpacesFromLines = false) {
        if (val instanceof Map) {
            return canonicalMap(val,trimSpacesFromLines)
        } else if (val instanceof String) {
            //set multiline strings to use unix line endings
            if(trimSpacesFromLines) return BuilderUtil.trimAllLinesAndReplaceLineEndings(val, DumperOptions.LineBreak.UNIX.getString())
            return BuilderUtil.replaceLineEndings(val, DumperOptions.LineBreak.UNIX.getString())
        } else if (val instanceof List) {
            return val.collect { canonicalValue(it,trimSpacesFromLines) }
        }
        return val
    }

    static Map canonicalMap(Map input, boolean trimSpacesFromLines = false) {
        def result = [:]//linked hash map has ordered keys
        input.keySet().sort().each {
            def val = input[it]
            result[it] = canonicalValue(val,trimSpacesFromLines)
        }
        result
    }

    /**
     * Extends a base Represent, to use SINGLE_QUOTED scalar style when
     * representing a String that contains a comma (',') character.
     * Prevents misinterpretation as numeric value on import in some locales
     */
    static class CommaStringQuotedRepresent implements Represent{
        Represent strRepresent

        CommaStringQuotedRepresent(final Represent strRepresent) {
            this.strRepresent = strRepresent
        }

        @Override
        Node representData(final Object data) {
            def data1 = strRepresent.representData(data)
            if(data instanceof String && data.indexOf(',')>=0 && data1.tag==Tag.STR && data1 instanceof ScalarNode){
                ScalarNode scalarNode=(ScalarNode)data1
                if (scalarNode.scalarStyle == DumperOptions.ScalarStyle.PLAIN) {
                    return new ScalarNode(scalarNode.tag, scalarNode.value, scalarNode.startMark, scalarNode.endMark,
                                          DumperOptions.ScalarStyle.SINGLE_QUOTED)
                }
            }
            return data1
        }
    }
    static class CommaStringQuotedRepresenter extends Representer{
        CommaStringQuotedRepresenter() {
            super(new DumperOptions())
            this.representers.put(String,new CommaStringQuotedRepresent(this.representers.get(String)))
        }
    }

    /**
     * Checks if the given notification map follows or should follow the old format
     * @param notificationMap canonical job map
     * @return false if notificationMap has more than one notification of the same type in any trigger
     */
    static boolean useOldFormat(Map notificationMap){
        boolean useOld

        def hasMany = notificationMap.find { Map.Entry triggerEntry ->
            Map notifsAmount = [:]
            return triggerEntry.value.find{ Map notifEntry ->
                def notifType = notifEntry.keySet()[0]

                if(notifsAmount[notifType] || (notifType == 'plugin' && (notifEntry[notifType] as List).size() > 1))
                    return notifEntry
                else
                    notifsAmount[notifType] = 1

                return
            }
        }
        useOld = hasMany?false:true
        return useOld
    }
    static Map performMapping(Options options, Map<String, Object> map){
        boolean shouldUseOldFormat = map['notification']? useOldFormat(map['notification'] as Map) : false
        if (options.replaceIds && options.replaceIds.get(map['id'])) {
            map['id'] = options.replaceIds.get(map.remove('id'))
            map['uuid'] = options.replaceIds.get(map.remove('uuid'))
        } else if (!options.preserveUuid) {
            map.remove('id')
            map.remove('uuid')
        }
        if(shouldUseOldFormat){
            (map['notification'] as Map).keySet().sort().findAll { (it as String).startsWith('on') }.each { trigger ->
                String trigger_ = trigger as String
                def notifs = map['notification'][trigger_]
                map['notification'][trigger_] = [:]
                notifs.each { Map it ->
                    if(it['plugin'])
                        it['plugin'] = (it['plugin'] as List)[0]
                    map['notification'][trigger_] = (map['notification'][trigger_] as Map) + it
                }
            }
        }
        map
    }

    @Override
    void encode(final List<Map> list, Options options, final Writer writer) {
        final DumperOptions dumperOptions = new DumperOptions()
        dumperOptions.lineBreak = DumperOptions.LineBreak.UNIX
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK)
        final Representer representer = new CommaStringQuotedRepresenter()
        Yaml yaml = new Yaml(representer,dumperOptions)


        yaml.dump(list.collect { canonicalMap(performMapping(options,it), trimSpacesFromLines) }, writer)
    }
}
