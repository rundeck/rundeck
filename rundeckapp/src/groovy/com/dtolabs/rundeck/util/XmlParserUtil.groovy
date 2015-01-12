package com.dtolabs.rundeck.util

import org.apache.commons.lang.StringUtils

/*
* Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
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
*
*/

/*
* XmlParserUtil.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 27, 2011 6:48:20 PM
*
*/

public class XmlParserUtil {
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
        def map = [:]
        if (data.text()) {
            map['<text>'] = analyze?XmlParserUtil.analyzeText(text):text
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
                    if (null != map[gp.name()] && !(map[gp.name()] instanceof Collection)) {
                        def v = map[gp.name()]
                        map[gp.name()] = [v, toObject(gp,analyze)]
                    } else if (map[gp.name()] instanceof Collection) {
                        map[gp.name()] << toObject(gp, analyze)
                    } else {
                        map[gp.name()] = toObject(gp, analyze)
                    }
                }
            }
        }
        if (1 == map.size() && null!=map['<text>']) {
            return map['<text>']
        }else if(0==map.size()){
            return ''
        }
        if(sawElems && null!=map['<text>']){
            //remove text if other sub elements
            map.remove('<text>')
        }
        return map
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
