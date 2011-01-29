import groovy.util.slurpersupport.GPathResult

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
    static Object toObject(Node data) {
        if (null == data) {
            return null
        }
        def childs = data.value()
        def text = data.text()
        def attrs = data.attributes()
        def map = [:]
        if (data.text()) {
            map['<text>'] = XmlParserUtil.analyzeText(text)
        }
        if (attrs) {
            attrs.keySet().each{
                map.put(it,analyzeText(attrs[it]))
            }
        }
        if (null != childs && childs instanceof Collection) {
            childs.each {gp ->
                if (gp instanceof Node) {
                    if (null != map[gp.name()] && !(map[gp.name()] instanceof Collection)) {
                        def v = map[gp.name()]
                        map[gp.name()] = [v, toObject(gp)]
                    } else if (map[gp.name()] instanceof Collection) {
                        map[gp.name()] << toObject(gp)
                    } else {
                        map[gp.name()] = toObject(gp)
                    }
                }
            }
        }
        if (1 == map.size() && null!=map['<text>']) {
            return map['<text>']
        }else if(0==map.size()){
            return ''
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
}