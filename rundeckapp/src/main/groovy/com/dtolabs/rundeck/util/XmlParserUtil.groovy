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

package com.dtolabs.rundeck.util

import org.apache.commons.lang.StringUtils
import org.rundeck.app.components.jobs.JobXMLUtil

/*
* XmlParserUtil.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 27, 2011 6:48:20 PM
*
*/

public class XmlParserUtil {
    public static final String TEXT_KEY = JobXMLUtil.TEXT_KEY
    Node data

    public XmlParserUtil(Node data) {
        this.data = data
    }

    /**
     * Given a node, produce a Map of [name: contents]
     */
    Map toMap() {
        return [(data.name()): toObject()]
    }
    /**
     * Given a node, produce a data value: map, list, set or string
     */
    Object toData() {
        return toDataValue(data)
    }

    /**
     * Given a node, produce contents object
     */
    Object toObject() {
        return XmlParserUtil.toObject(data)
    }

    /**
     * Generate data object from the node, not including the node name
     */
    static Object toObject(Node data, boolean analyze=true) {
        if (null == data) {
            return null
        }
        def childs = data.value()
        def text = data.text()
        def attrs = data.attributes()
        if (attrs['data'] == 'true' && childs instanceof Collection && childs.size()==1) {
            return toDataValue(childs[0])
        }
        def map = [:]
        if (data.text()) {
            map[TEXT_KEY] = analyze ? XmlParserUtil.analyzeText(text) : text
        }
        if (attrs) {
            attrs.keySet().each{
                map.put(it,analyze?analyzeText(attrs[it]):attrs[it])
            }
        }
        def sawElems=false
        if (null != childs && childs instanceof Collection) {
            childs.each {gp ->
                if (gp instanceof Node) {
                    sawElems=true
                    def name = gp.name()
                    if(name=='element' && gp.attributes()['name']){
                        name=gp.attributes()['name']
                        gp.attributes().remove('name')
                    }
                    if (null != map[name] && !(map[name] instanceof Collection)) {
                        def v = map[name]
                        map[name] = [v, toObject(gp, analyze)]
                    } else if (map[name] instanceof Collection) {
                        map[name] << toObject(gp, analyze)
                    } else {
                        map[name] = toObject(gp, analyze)
                    }
                }
            }
        }
        if (1 == map.size() && null!=map[TEXT_KEY]) {
            return map[TEXT_KEY]
        }else if(0==map.size()){
            return ''
        }
        if(sawElems && null!=map[TEXT_KEY]){
            //remove text if other sub elements
            map.remove(TEXT_KEY)
        }
        return map
    }
    /**
     * Generate data object from the node, not including the node name,
     * reverse the result of {@link com.dtolabs.rundeck.app.support.BuilderUtil#dataObjToDom(java.lang.Object, java.lang.Object)}
     */
    static Object toDataValue(Node data) {
        if (data.name() == 'map') {
            def map = [:]
            data.children().each { entry ->
                if (entry['@key']) {
                    map[entry['@key']] = toDataValue(entry)
                }
            }
            return map
        } else if (data.name() == 'list') {
            List list = []
            data.children().each { entry ->
                list << toDataValue(entry)
            }
            return list
        } else if (data.name() == 'set') {
            Set set = new HashSet()
            data.children().each { entry ->
                set << toDataValue(entry)
            }
            return set
        } else if (data.name() == 'value') {
            if (data.children().size() == 1) {
                def value = data.children()[0]
                return value.toString()
            }
            return null
        }
    }
    static Object analyzeText(String text){
        if(text=~/^\d+$/){
            return Integer.parseInt(text)
        }else if(text=~/^(?i:true|false)$/){
            return Boolean.parseBoolean(text)
        }
        return text
    }

    static int stringToInt(Object obj, int defValue) {
        if (null != obj && obj instanceof Integer) {
            return obj
        } else if (null != obj && obj instanceof String && !StringUtils.isBlank(obj)) {
            try {
                return Integer.parseInt((String) obj)
            } catch (NumberFormatException e) {
            }
        }
        return defValue
    }

    static boolean stringToBool(Object obj, boolean defValue) {
        if (null != obj && obj instanceof Boolean) {
            return obj
        } else if (null != obj && obj instanceof String && !StringUtils.isBlank(obj)) {
            return Boolean.parseBoolean((String) obj)
        }
        return defValue
    }
}
