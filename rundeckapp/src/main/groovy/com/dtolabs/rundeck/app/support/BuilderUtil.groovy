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

package com.dtolabs.rundeck.app.support

import org.rundeck.app.components.jobs.JobXMLUtil

import java.util.regex.Pattern

/**
 * BuilderUtil assists in generating XML or any groovy builder structure
 * from a standard data structure consisting of Maps, Collections and Strings/other objects.
 *
 * Without any modifications, an XML builder would produce a document by:<br/>
 * <li>converting each String map key into an element
 * <li>converting each String value into text content (*see note)
 * <li>converting each Collection value into a sequence of elements with the same key (*see note)
 * <li>converting each Map into a sequence of elements
 *
 * This process can be modified to support XML Attributes, and to support a slightly different mechanism
 * for collections.<br>
 *
 * Attributes can be created by modifying the key to have a prefix of "@attr:".  This can be done with the
 * {@link BuilderUtil#addAttribute(Map, String, Object)  } or {@link BuilderUtil#asAttributeName(String)  } or {@link BuilderUtil#makeAttribute(Map, String)  } methods.
 *
 * Collections can be wrapped in a plural/single form by using a certain suffix for a key.  If the key ends with "[s]"
 * then the collection is treated in this way:
 * <li>A singular key name is created by removing the "[s]" suffix.</li>
 * <li>An element is created using the singular key name with "s" appended to it.</li>
 * <li>Each item in the collection is then processed under the enclosing renamed element, using the singular key
 * as the element name<li>
 *
 * This allows naturally named data such as [myvalues:[1,2,3]] to produce:
 *
 * <pre>
 *    &lt;myvalues&gt;
 *       &lt;myvalue&gt;1&lt;myvalue&gt;
 *       &lt;myvalue&gt;2&lt;myvalue&gt;
 *       &lt;myvalue&gt;3&lt;myvalue&gt;
 *    &lt;/myvalues&gt;
 * </pre>
 * 
 * The original key can be configured this way using the {@link BuilderUtil#makePlural(Map, String) method.
 *
 * The contents of an element can be serialized as a CDATA by another mechanism.  If the key ends with "<cdata>"
 * then the "<cdata>" suffix is removed, and the string contents serialized in a CDATA section.  {@link BuilderUtil#asCDATAName(String) }
 * will return the correct cdata key name from the original key.
 *
 */
class BuilderUtil {

    public static ATTR_PREFIX = JobXMLUtil.ATTR_PREFIX
    public static PLURAL_SUFFIX = JobXMLUtil.PLURAL_SUFFIX
    public static PLURAL_REPL = JobXMLUtil.PLURAL_REPL
    public static CDATA_SUFFIX = JobXMLUtil.CDATA_SUFFIX
    public static DATAVALUE_SUFFIX = JobXMLUtil.DATAVALUE_SUFFIX
    public static NEW_LINE = System.getProperty('line.separator')
    Map<Class, Closure> converters = [:]
    ArrayList context
    boolean canonical = false
    String lineEndingChars = NEW_LINE
    boolean automaticMultilineCdata = true
    /**
     * If true, replace all line endings in string output with the value of lineEndingChars
     */
    boolean forceLineEndings = false
    public BuilderUtil(){
        context=new ArrayList()
    }

    public mapToDom( Map map, builder){
        //generate a builder strucure using the map components
        for(Object o: canonical?map.keySet().sort():map.keySet()){
            final Object val = map.get(o)
            this.objToDom(o,val,builder)
        }
    }

    static def toValidKey(key) {
        if (!isNcName(key)) {
            return ['element', [name: key]]
        }
        [key, [:]]
    }

    /**
     * Encode generic data structure into xml,
     * @param key
     * @param obj
     * @param builder
     * @see {@link com.dtolabs.rundeck.util.XmlParserUtil#toDataValue(groovy.util.Node)}
     */
    public dataObjToDom(obj, builder, attrs = [:]) {
        if (obj instanceof Map) {
            //encode as <map><entry key="name">$value</entry></map>
            builder.map(attrs) {
                obj.each { k, v ->
                    dataObjToDom(v, builder, [key: k])
                }
            }
        } else if (obj instanceof List) {
            //encode as <list><entry>$value</entry></list>
            builder.list(attrs) {
                obj.each { v ->
                    dataObjToDom(v, builder)
                }
            }
        } else if (obj instanceof Collection) {
            //encode as <set><entry>$value</entry></set>
            builder.set(attrs) {
                obj.each { v ->
                    dataObjToDom(v, builder)
                }
            }
        } else if (obj.metaClass.respondsTo(obj, 'toMap')) {
            dataObjToDom(obj.toMap(), builder, attrs)
        } else {
            //string case or toString
            String str = obj.toString()
            //encode as <value>$string</value>
            if (automaticMultilineCdata && str.indexOf(lineEndingChars) >= 0) {
                builder."value"(attrs) {
                    mkp.yieldUnescaped("<![CDATA[" + str.replaceAll(']]>', ']]]]><![CDATA[>') + "]]>")
                }
            } else {
                builder."value"(attrs, str)
            }
        }
    }
    public objToDom(key,obj,builder){
        def elemAttrs = [:]
        if (key.endsWith(DATAVALUE_SUFFIX)) {
            (key, elemAttrs) = toValidKey(key - DATAVALUE_SUFFIX)
            builder."$key"(elemAttrs + ['data': true]) {
                dataObjToDom(obj, builder)
            }
            return
        }
        if(null==obj){
            (key, elemAttrs) = toValidKey(key)
            builder."${key}"(elemAttrs)
        }else if (obj instanceof Collection){
            //iterate
            def cobj = (Collection) obj
            if(key instanceof String && ((String)key).length()>1 && ((String)key).endsWith(PLURAL_SUFFIX)){
                String keys=(String)key
                String name=keys.substring(0,keys.size()-PLURAL_SUFFIX.size());
                String rekey=name+PLURAL_REPL;

                (rekey, elemAttrs) = toValidKey(rekey)
                builder."${rekey}"(elemAttrs) {
                    for(Object o: cobj){
                        this.objToDom(name,o,builder)
                    }
                }
            }else{
                for(Object o: cobj){
                    this.objToDom(key,o,builder)
                }
            }
        }else if(obj instanceof Map){
            (key, elemAttrs) = toValidKey(key)
            //try to collect '@' prefixed keys to apply as attributes
            Map map = (Map)obj
            def keys = canonical?map.keySet().sort():map.keySet()
            def attrs = keys.findAll{it=~/^${ATTR_PREFIX}/}
            def attrmap=[:]
            if(attrs){
                attrs.each{String s->
                    def x =s.substring(ATTR_PREFIX.length())
                    attrmap[x]=map.remove(s)
                }
            }
            if (map.size() == 1 && null != map[JobXMLUtil.TEXT_KEY]) {
                builder."${key}"(elemAttrs + attrmap, map[JobXMLUtil.TEXT_KEY])
            } else {
                builder."${key}"(elemAttrs + attrmap) {
                    this.mapToDom(map, delegate)
                }
            }
        }else if(obj.metaClass.respondsTo(obj,'toMap')){
            (key, elemAttrs) = toValidKey(key)
            def map = obj.toMap()
            builder."${key}"(elemAttrs) {
                this.mapToDom(map,delegate)
            }
        }else {
            String os
            if(converters[obj.class]){
                os=converters[obj.class].call(obj)
            }else{
                os=obj.toString()
            }
            if(forceLineEndings) {
                os = replaceLineEndings(os,lineEndingChars)
            }
            if(key.endsWith(CDATA_SUFFIX) || (automaticMultilineCdata && os.indexOf(lineEndingChars)>=0)){
                (key, elemAttrs) = toValidKey(key - CDATA_SUFFIX)
                builder."${key}"(elemAttrs) {
                    mkp.yieldUnescaped("<![CDATA["+os.replaceAll(']]>',']]]]><![CDATA[>')+"]]>")
                }
            }else{
                (key, elemAttrs) = toValidKey(key)
                builder."${key}"(elemAttrs, os)
            }
        }
    }

    /**
     * Replace all line endings with the given string
     * @param os input string
     * @param lineEnding line ending string to use
     * @return new string
     */
    public static String replaceLineEndings(String os, String lineEnding) {
        os.replaceAll('(\r\n|\r|\n)', lineEnding)
    }

    /**
     * Add entry to the map for the given key, converting the key into an
     * attribute key identifier
     */
    public static addAttribute(Map map, String key, val) {
        JobXMLUtil.addAttribute(map, key, val)
    }

    /**
     * Return the key as an attribute key identifier
     */
    public static String asAttributeName(String key) {
        return JobXMLUtil.asAttributeName(key)
    }

    /**
     * Replace the key in the map with the attribute key identifier,
     * if the map entry exists and is not null
     */
    public static makeAttribute(Map map, String key) {
        JobXMLUtil.makeAttribute(map, key)
    }


    /**
     * Return a Map with an attribute key identifier created from
     * the given key, and the given value 
     */
    public static Map toAttrMap(String key, val) {
        JobXMLUtil.toAttrMap(key, val)
    }

    /**
     * Return the pluralized key form of the key
     */
    public static String pluralize(String key) {
        JobXMLUtil.pluralize(key)
    }

    /**
     * change the key for the map entry to the pluralized key form
     */
    public static Map makePlural(Map map, String key) {
        JobXMLUtil.makePlural(map, key)
    }

    /**
     * Return the key name for use as a CDATA section
     */
    public static String asCDATAName(String key) {
        JobXMLUtil.asCDATAName(key)
    }
    /**
     * Return the key name for use as generic data structure
     */
    public static String asDataValueKey(String key){
        JobXMLUtil.asDataValueKey(key)
    }
    //	NameStartChar	   ::=   	":" | [A-Z] | "_" | [a-z] | [#xC0-#xD6] | [#xD8-#xF6] | [#xF8-#x2FF] |
    //	[#x370-#x37D] | [#x37F-#x1FFF] | [#x200C-#x200D] | [#x2070-#x218F] | [#x2C00-#x2FEF] | [#x3001-#xD7FF] |
    //	[#xF900-#xFDCF] | [#xFDF0-#xFFFD] | [#x10000-#xEFFFF]
    //  [4a]   	NameChar	   ::=   	NameStartChar | "-" | "." | [0-9] | #xB7 | [#x0300-#x036F] |
    // [#x203F-#x2040]
    // from  https://www.w3.org/TR/REC-xml-names/#NT-NCName
    // [4]   	NCName	   ::=   	Name - (Char* ':' Char*)	/* An XML Name, minus the ":" */
    static final String NC_NAME_START =/*skip : char */
            'a-zA-Z_\\x{00C0}-\\x{00D6}' +
            '\\x{00D8}-\\x{00F6}' +
            '\\x{00F8}-\\x{02FF}' +
            '\\x{0370}-\\x{037D}' +
            '\\x{037F}-\\x{1FFF}' +
            '\\x{200C}-\\x{200D}' +
            '\\x{2070}-\\x{218F}' +
            '\\x{2C00}-\\x{2FEF}' +
            '\\x{3001}-\\x{D7FF}' +
            '\\x{F900}-\\x{FDCF}' +
            '\\x{FDF0}-\\x{FFFD}' +
            '\\x{10000}-\\x{EFFFF}'
    static final String NAME = '-.0-9\\x{00B7}\\x{0300}-\\x{036F}\\x{203F}-\\x{2040}'
    static final Pattern NC_NAME_PATTERN = Pattern.compile('[' + NC_NAME_START + NAME + ']')

    /**
     *
     * @param key
     * @return true if key is a valid <a href="https://www.w3.org/TR/REC-xml-names/#NT-NCName">NCName</a>
     */
    static boolean isNcName(String key) {
        key.chars.every {
            it ==~ NC_NAME_PATTERN
        }
    }
}
