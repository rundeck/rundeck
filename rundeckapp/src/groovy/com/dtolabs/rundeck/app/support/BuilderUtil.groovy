package com.dtolabs.rundeck.app.support
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
class BuilderUtil{

    public static ATTR_PREFIX="@attr:"
    public static PLURAL_SUFFIX="[s]"
    public static PLURAL_REPL="s"
    public static CDATA_SUFFIX="<cdata>"
    public static NEW_LINE = System.getProperty('line.separator')
    Map<Class,Closure> converters=[:]
    ArrayList context
    boolean canonical=false
    String lineEndingChars = NEW_LINE
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
    public objToDom(key,obj,builder){
        if(null==obj){
            builder."${key}"()
        }else if (obj instanceof Collection){
            //iterate
            def cobj = (Collection) obj
            if(key instanceof String && ((String)key).length()>1 && ((String)key).endsWith(PLURAL_SUFFIX)){
                String keys=(String)key
                String name=keys.substring(0,keys.size()-PLURAL_SUFFIX.size());
                String rekey=name+PLURAL_REPL;
                builder."${rekey}"(){
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
            if (map.size() == 1 && null != map['<text>']) {
                builder."${key}"(attrmap, map['<text>'])
            } else {
                builder."${key}"(attrmap) {
                    this.mapToDom(map, delegate)
                }
            }
        }else if(obj.metaClass.respondsTo(obj,'toMap')){
            def map = obj.toMap()
            builder."${key}"(){
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
            if(key.endsWith(CDATA_SUFFIX)){
                builder."${key-CDATA_SUFFIX}"(){
                    mkp.yieldUnescaped("<![CDATA["+os.replaceAll(']]>',']]]]><![CDATA[>')+"]]>")
                }
            }else{
                builder."${key}"(os)
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
    public static addAttribute(Map map,String key,val){
        map[asAttributeName(key)]=val
    }

    /**
     * Return the key as an attribute key identifier
     */
    public static String asAttributeName(String key) {
        return ATTR_PREFIX + key
    }

    /**
     * Replace the key in the map with the attribute key identifier,
     * if the map entry exists and is not null
     */
    public static makeAttribute(Map map,String key){
        if(null!=map){
            final Object remove = map.remove(key)
            if(null!=remove){
                map[asAttributeName(key)]=remove
            }
        }
    }


    /**
     * Return a Map with an attribute key identifier created from
     * the given key, and the given value 
     */
    public static Map toAttrMap(String key, val){
        def map=[:]
        if(null!=key){
            map[asAttributeName(key)]=val
        }
        return map
    }

    /**
     * Return the pluralized key form of the key
     */
    public static String pluralize(String key){
       if(key.endsWith(PLURAL_SUFFIX)){
           return key
       }else if(key.endsWith(PLURAL_REPL)){
           def k=key.substring(0,key.size()-PLURAL_REPL.size());
           return k+PLURAL_SUFFIX
       }
       return key+PLURAL_SUFFIX
    }

    /**
     * change the key for the map entry to the pluralized key form
     */
    public static Map makePlural(Map map, String key){
        map[pluralize(key)]=map.remove(key)
        return map
    }

    /**
     * Return the key name for use as a CDATA section
     */
    public static String asCDATAName(String key){
        return key+CDATA_SUFFIX
    }
}
