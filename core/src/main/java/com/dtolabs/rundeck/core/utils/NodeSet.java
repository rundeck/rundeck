/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.utils;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.common.NodesSelector;
import com.dtolabs.rundeck.core.common.SelectorUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.*;
import java.util.regex.PatternSyntaxException;


/**
 * NodeSet provides filtering logic for Node criteria
 */
public class NodeSet  implements NodesSelector {
    public static final String HOSTNAME = "hostname";
    public static final String NAME = "name";
    public static final String TYPE = "type";
    public static final String TAGS = "tags";
    public static final String OS_NAME = "os-name";
    public static final String OS_FAMILY = "os-family";
    public static final String OS_ARCH = "os-arch";
    public static final String OS_VERSION = "os-version";

    private String singleNodeName;
    public static final String DEFAULT_FILTER_KEY = "name";

    /**
     * default constructor
     */
    public NodeSet() {
    }

    /**
     * Create a nodeset with a single node name
     * @param singleNodeName node name
     */
    public NodeSet(final String singleNodeName) {
        this.singleNodeName = singleNodeName;
    }

    /**
     * Create a nodeset for a single node
     * @param singleNode the node
     */
    public NodeSet(final INodeEntry singleNode) {
        this(singleNode.getNodename());
    }

    /**
     * names of the filter attributes used by exclude/include that are exposed to the CLI interface
     */
    private static final String[] FILTER_KEYS = {
        HOSTNAME,
        NAME,
        TYPE,
        TAGS,
        OS_NAME,
        OS_FAMILY,
        OS_ARCH,
        OS_VERSION,
    };
    /**
     * names of the filter attributes used by exclude/include that are exposed to the CLI interface
     */
    public static final List<String> FILTER_KEYS_LIST = Collections.unmodifiableList(Arrays.asList(FILTER_KEYS));

    public String getSingleNodeName() {
        return singleNodeName;
    }

    public void setSingleNodeName(final String singleNodeName) {
        this.singleNodeName = singleNodeName;
    }

    public boolean acceptNode(final INodeEntry entry) {
        return !shouldExclude(entry);
    }
    /**
     * @return a new Node Selector that will apply the include/exclude filters if they are set, otherwise
     * it only includes the node with the given nodename
     * @param nodename name
     */
    public NodesSelector nodeSelectorWithDefault(final String nodename) {
        final NodesSelector nodesSelector = SelectorUtils.singleNode(nodename);
        return new NodesSelector() {
            public boolean acceptNode(final INodeEntry entry) {
                return !isBlank() ? NodeSet.this.acceptNode(entry) : nodesSelector.acceptNode(entry);
            }
        };
    }
    /**
     * @return a new Node Selector that will apply the include/exclude filters if they are set, otherwise
     * it will accept all nodes
     */
    public NodesSelector nodeSelectorWithDefaultAll() {
        return new NodesSelector() {
            public boolean acceptNode(final INodeEntry entry) {
                return isBlank() || NodeSet.this.acceptNode(entry);
            }
        };
    }

    /**
     * Enum of filters.
     * Implements a visitor pattern for SetSelector instances, allowing the enum to get the appropriate property
     * value of a SetSelector instances with the {@link #value(NodeSet.SetSelector)}
     * method.
     */
    public static enum FILTER_ENUM{

        F_HOSTNAME ("hostname"){public String value(SetSelector set) { return set.getHostname(); }},
        F_NAME ("name"){public String value(SetSelector set) { return set.getName(); }},
        F_TAGS ("tags"){public String value(SetSelector set) { return set.getTags(); }},
        F_OS_NAME ("os-name"){public String value(SetSelector set) { return set.getOsname(); }},
        F_OS_FAMILY("os-family"){public String value(SetSelector set) { return set.getOsfamily(); }},
        F_OS_ARCH ("os-arch"){public String value(SetSelector set) { return set.getOsarch(); }},
        F_OS_VERSION ("os-version"){public String value(SetSelector set) { return set.getOsversion(); }};

        private final String name;
        FILTER_ENUM(String name){
            this.name=name;
        }
        public String getName(){
            return name;
        }

        /**
         * Return the value of the property matching the filter key for the given selector set
         * @param set the SetSelector
         * @return the property value
         */
        public abstract String value(SetSelector set);
    }

    /**
     * Collection of names of attributes used by exclude/include that are exposed to the CLI interface
     */
    public static final Collection<String> FILTER_KEYSET = new HashSet<String>(FILTER_KEYS_LIST);
    private Include includes;

    private Exclude excludes;
    private int threadCount=1;
    private boolean keepgoing=false;
    private File failedNodesfile;

    /**
     * Return true if the include/exclude filters are blank or non-existent
     * @return true if all filters are blank or empty
     */
    public boolean isBlank() {
        return (null == getInclude() || getInclude().isBlank()) && (null == getExclude() || getExclude().isBlank()) && null==getSingleNodeName();
    }
    public Include createInclude() {
        if (null != includes) {
            throw new IllegalStateException("only one include is allowed");
        }
        includes = new Include();
        return includes;
    }

    public Exclude createExclude() {
        if(null!=excludes) {
            throw new IllegalStateException("only one exclude is allowed");
        }
        excludes = new Exclude();
        return excludes;
    }

    public Include getInclude() {
        return includes;
    }

    public Exclude getExclude() {
        return excludes;
    }

    /**
     * Return true if the node entry should be excluded based on the includes and excludes parameters.
     * When both include and exclude patterns match the node, it will be excluded based on which filterset is dominant.
     * @param entry node descriptor entry
     * @return true if the node should be excluded.
     */
    public boolean shouldExclude(final INodeEntry entry) {
        if(null!=getSingleNodeName()) {
            return !getSingleNodeName().equals(entry.getNodename());
        }

        boolean includesMatch = includes != null && includes.matches(entry);
        boolean excludesMatch = excludes != null && excludes.matches(entry);
        if (null==excludes ||excludes.isBlank()) {
            return !includesMatch;
        } else if (null==includes || includes.isBlank()) {
            return excludesMatch;
        } else if(null!=includes && includes.isDominant()) {
            return !includesMatch && excludesMatch;
        }else{
            return !includesMatch || excludesMatch; 
        }
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public boolean isKeepgoing() {
        return keepgoing;
    }

    public void setKeepgoing(boolean keepgoing) {
        this.keepgoing = keepgoing;
    }

    /**
     * Validate input. If failedNodesfile looks like an invalid property reference, set it to null.
     */
    public void validate() {
        if (null != failedNodesfile && failedNodesfile.getName().startsWith("${") && failedNodesfile.getName()
                .endsWith("}")) {
            failedNodesfile=null;
        }
    }
    /**
     * @return true if the input selector matches the specified property value
     * @param inputSelector input
     * @param propValue value
     */
    protected static boolean matchesInput(String inputSelector, String propValue) {
        if (null == propValue || "".equals(propValue.trim())) {
            return false;
        }
        if (null == inputSelector || "".equals(inputSelector.trim())) {
            return false;
        }
        List list = Arrays.asList(inputSelector.split(","));
        return matchRegexOrEquals(inputSelector,propValue) || list.contains(propValue);

    }


    /**
     * @return true if any attribute selector matches the corresponding attribute value
     * @param matchAll if true, require all selectors match a value, otherwise return true if any selector matches
     * @param attrSelectors map of attribute key to selector
     * @param values map of key to value
     */
    protected static boolean matchesInput(Map<String,String> attrSelectors, Map<String,String> values, boolean matchAll) {
        if (null == attrSelectors || null == values) {
            return false;
        }
        for (final Map.Entry<String, String> entry : attrSelectors.entrySet()) {
            final boolean match = matchesInput(entry.getValue(), values.get(entry.getKey()));
            if (!match && matchAll) {
                return false;
            } else if (match && !matchAll) {
                return true;
            }
        }
        //if matchAll, then all matches succeeded by now. return true, == matchAll.
        //if !matchAll, then no match succeeded by now. return false == matchAll.
        return matchAll;
    }

    /**
     * @return true if the input string is found in the set of values.
     * The inputSelector can contain boolean and using the "+" symbol, and boolean or using the "," symbol.
     * E.g.:  "a + b" - matches if both "a" and "b" are in the value set
     * E.g.:  "a , b" - matches if either "a" or "b" are in the value set
     *
     * @param inputSelector input
     * @param propSet values
     */
    static boolean matchesInputSet(String inputSelector, Collection propSet) {
        if (null == propSet || propSet.size()<1) {
            return false;
        }
        if(null==inputSelector || "".equals(inputSelector.trim())){
            return false;
        }
        if (inputSelector.contains("+") || inputSelector.contains(",")) {
            HashSet orSet = new HashSet();
            orSet.addAll(Arrays.asList(inputSelector.split(",")));
            for (Object anOrSet : orSet) {
                String clause = (String) anOrSet;
                HashSet set = new HashSet();
                set.addAll(Arrays.asList(clause.split("\\+")));
                boolean found = true;
                for (Object aSet : set) {
                    String tag = (String) aSet;

                    if (!propSet.contains(tag.trim())) {
                        //try regular expression match
                        boolean rematch = false;
                        for (Object aPropSet : propSet) {
                            String item = (String) aPropSet;
                            if (matchRegexOrEquals(tag, item)) {
                                rematch = true;
                                break;
                            }
                        }
                        if (!rematch) {
                            found = false;
                        }
                    }
                }
                if (found) {
                    return true;
                }
            }
            return false;
        }else {
            boolean contain = propSet.contains(inputSelector);
            boolean rematch = false;
            for (Object aPropSet : propSet) {
                String item = (String) aPropSet;
                if (matchRegexOrEquals(inputSelector, item)) {
                    rematch = true;
                    break;
                }
            }
            return rematch || contain;
        }
    }

    /**
     * Tests whether the selector string matches the item, in three possible ways: first, if the selector looks like:
     * "/.../" then it the outer '/' chars are removed and it is treated as a regular expression *only* and
     * PatternSyntaxExceptions are not caught.  Otherwise, it is treated as a regular expression and any
     * PatternSyntaxExceptions are caught. If it does not match or if the pattern is invalid, it is tested for string
     * equality with the input.
     *
     * @param inputSelector test string which may be a regular expression, or explicit regular expression string wrapped
     *                      in '/' characters
     * @param item          item to test
     *
     * @return true if the item matches the selector
     */
    public static boolean matchRegexOrEquals(final String inputSelector, final String item) {
        //see if inputSelector is wrapped in '/' chars
        String testregex = inputSelector;
        if (testregex.length()>=2 && testregex.indexOf('/') == 0 && testregex.lastIndexOf('/') == testregex.length() - 1) {
            testregex = inputSelector.substring(1, inputSelector.length() - 1);
            return item.matches(testregex.trim());
        }
        boolean match = false;
        try {
            match = item.matches(inputSelector.trim());
        } catch (PatternSyntaxException e) {

        }
        return match || inputSelector.trim().equals(item);
    }

    /**
     * Create a NodeSet from a filter
     * @param filter filter string
     * @return node set
     */
    public static NodeSet fromFilter(String filter) {
        Map<String, Map<String, String>> stringMapMap = parseFilter(filter);
        NodeSet nodeSet = new NodeSet();
        nodeSet.createInclude(stringMapMap.get("include"));
        nodeSet.createExclude(stringMapMap.get("exclude"));
        return nodeSet;
    }
    /**
     * Parse textual filter and return a map of maps: [include: [key:value,...], exclude: [key:value,...]]
     *
     * @param filter filter string
     *
     * @return parsed filter
     */
    public static Map<String,Map<String, String>> parseFilter(String filter) {
        return parseFilter(filter, true, DEFAULT_FILTER_KEY);
    }
    public static Map<String,Map<String, String>> parseFilter(String filter, final boolean joinMulti, final String defaultKey) {
        Map<String, String> exclude = new HashMap<String, String>();
        Map<String, String> include = new HashMap<String, String>();
        HashMap<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
        result.put("include", include);
        result.put("exclude", exclude);

        String[] burst = OptsUtil.burst(filter);
        String key = null;
        for (int i = 0; i < burst.length; i++) {
            String t = burst[i];
            if (t.endsWith(":") && key == null) {
                key = t.substring(0, t.lastIndexOf(':'));
                continue;
            } else if (t.contains(":") && key == null) {
                key = t.substring(0, t.indexOf(':'));
                t = t.substring(t.indexOf(':') + 1);
                if ("".equals(t)) {
                    continue;
                }
            }else if(key==null && null!= defaultKey){
                //default filter key
                key= defaultKey;
            }
            if (key != null) {
                Map<String, String> set=include;
                if (key.startsWith("!")) {
                    key = key.substring(1);
                    set = exclude;
                }
                if (null != set.get(key) && joinMulti) {
                    set.put(key, set.get(key) + "," + t);
                } else {
                    set.put(key, t);
                }
                key = null;
            } else {
                key = t;
            }
        }
        return result;
    }
    public static String generateFilter(NodeSet set) {
        ArrayList<String> buf = new ArrayList<String>();
        Map<String, String> incMap = set.includes.toMap();
        for (String inc : incMap.keySet()) {
            buf.add(inc + ":");
            buf.add(incMap.get(inc));
        }
        Map<String, String> excMap = set.excludes.toMap();
        for (String inc : excMap.keySet()) {
            buf.add("!" + inc + ":");
            buf.add(excMap.get(inc));
        }
        return OptsUtil.join(buf);
    }
    /**
     * Creates an {@link Exclude} object populating its
     * properties from the keys of the map
     * @param excludeMap Map containing nodes.properties data
     * @return new Exclude object configured for use in this NodeSet
     */
    public Exclude createExclude(Map excludeMap) {
        Exclude exclude = createExclude();
        populateSetSelector(excludeMap, exclude);
        return exclude;
    }
    /**
     * Creates an {@link Include} object populating its
     * properties from the keys of the map
     * @param includeMap Map containing nodes.properties data
     * @return new Exclude object configured for use in this NodeSet
     */
    public Include createInclude(Map includeMap) {
        Include include = createInclude();
        populateSetSelector(includeMap, include);
        return include;
    }

    /**
     * Creates an {@link SetSelector} object populating its
     * properties from the keys of the map
     *
     * @param map         Map containing nodes.properties data
     * @param setselector include or exclude object
     * @return new SetSelector object configured for use in this NodeSet
     */
    public SetSelector populateSetSelector(Map map, SetSelector setselector) {
        HashMap<String,String> attrs = new HashMap<String, String>();
        for (final Object o : map.entrySet()) {
            final Map.Entry entry = (Map.Entry) o;
            final String key = (String) entry.getKey();
            final String value = asString(entry.getValue(), ",");
            if (HOSTNAME.equals(key)) {
                setselector.setHostname(value);
            } else if (OS_FAMILY.equals(key)) {
                setselector.setOsfamily(value);
            } else if (OS_ARCH.equals(key)) {
                setselector.setOsarch(value);
            } else if (OS_NAME.equals(key)) {
                setselector.setOsname(value);
            } else if (OS_VERSION.equals(key)) {
                setselector.setOsversion(value);
            } else if (NAME.equals(key)) {
                setselector.setName(value);
            } else if (TAGS.equals(key)) {
                setselector.setTags(value);
            } else {
                attrs.put(key, value);
            }
        }
        setselector.setAttributesMap(attrs);
        return setselector;
    }

    private String asString(Object value, String delim) {
        if(null==value){
            return null;
        }
        if(value instanceof String){
            return (String) value;
        }else if(value instanceof String[]) {
            String[] s = (String[]) value;
            return StringArrayUtil.asString(s, delim);
        }else if(value instanceof Collection) {
            return StringUtils.join((Collection) value, delim);
        }else {
            return value.toString();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NodeSet nodeSet = (NodeSet) o;

        if (keepgoing != nodeSet.keepgoing) {
            return false;
        }
        if (threadCount != nodeSet.threadCount) {
            return false;
        }
        if (excludes != null ? !excludes.equals(nodeSet.excludes) : nodeSet.excludes != null) {
            return false;
        }
        if (includes != null ? !includes.equals(nodeSet.includes) : nodeSet.includes != null) {
            return false;
        }
        if (singleNodeName != null ? !singleNodeName.equals(nodeSet.singleNodeName) : nodeSet.singleNodeName != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = singleNodeName != null ? singleNodeName.hashCode() : 0;
        result = 31 * result + (includes != null ? includes.hashCode() : 0);
        result = 31 * result + (excludes != null ? excludes.hashCode() : 0);
        result = 31 * result + threadCount;
        result = 31 * result + (keepgoing ? 1 : 0);
        return result;
    }

    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("NodeSet{");
        if (null != excludes && !excludes.isBlank()) {
            builder.append("excludes=").append(excludes).append(", ");
        }
        if (null != includes && !includes.isBlank()) {
            builder.append("includes=").append(includes);
        }
        if (null != singleNodeName ) {
            builder.append("singleNode=").append(singleNodeName);
        }
        builder.append("}");
        return builder.toString();
    }

    public File getFailedNodesfile() {
        return failedNodesfile;
    }

    public void setFailedNodesfile(File failedNodesfile) {
        this.failedNodesfile = failedNodesfile;
    }

    /**
     * SetSelector is a filter
     */
    public abstract class SetSelector {
        private boolean dominant=false;
        private String hostname = "";
        private String osfamily = "";
        private String osarch = "";
        private String osname = "";
        private String tags = "";
        private String osversion = "";
        private String name = "";
        private Map<String,String> attributesMap;

        protected SetSelector() {
            attributesMap = new HashMap<String, String>();
        }

        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        public String getHostname() {
            return hostname;
        }

        public String toString() {
            final StringBuilder builder = new StringBuilder();
            builder.append("{");
            if(!isBlank(hostname)){
                builder.append("hostname=").append(getHostname()).append(", ");
            }
            if(!isBlank(osfamily)){
                builder.append("osfamily=").append(getOsfamily()).append(", ");
            }
            if (!isBlank(osarch)) {
                builder.append("osarch=").append(getOsarch()).append(", ");
            }
            if (!isBlank(osname)) {
                builder.append("osname=").append(getOsname()).append(", ");
            }
            if (!isBlank(osversion)) {
                builder.append("osversion=").append(getOsversion()).append(", ");
            }
            if (!isBlank(tags)) {
                builder.append("tags=").append(getTags()).append(", ");
            }
            if (!isBlank(name)) {
                builder.append("name=").append(getName()).append(", ");
            }
            builder.append("dominant=").append(isDominant()).append(", ");

            if (null != getAttributesMap() && getAttributesMap().size() > 0) {
                builder.append("attributesMap=").append(getAttributesMap());
            }
            builder.append("}");
            return builder.toString();
        }
        public Map<String,String> toMap(){
            HashMap<String, String> map = new HashMap<String, String>();
            if (!isBlank(hostname)) {
                map.put("hostname", getHostname());
            }
            if (!isBlank(osfamily)) {
                map.put("osfamily", getOsfamily());
            }
            if (!isBlank(osarch)) {
                map.put("osarch", getOsarch());
            }
            if (!isBlank(osname)) {
                map.put("osname", getOsname());
            }
            if (!isBlank(osversion)) {
                map.put("osversion", getOsversion());
            }
            if (!isBlank(tags)) {
                map.put("tags", getTags());
            }
            if (!isBlank(name)) {
                map.put("name", getName());
            }
//            builder.append("dominant=").append(isDominant()).append(", ");

            if (null != getAttributesMap() && getAttributesMap().size() > 0) {
                map.putAll(getAttributesMap());

            }
            return map;
        }

        public boolean isBlank(String value) {
            return (null == value || "".equals(value.trim()));
        }
        public boolean isBlank() {
            return isBlank(hostname)
                   && isBlank(osfamily)
                   && isBlank(osarch)
                   && isBlank(osname)
                   && isBlank(tags)
                   && isBlank(osversion)
                   && isBlank(name)
                   && isBlank(getAttributesMap())
                ;
        }
        public boolean isBlank(Map map) {
            return null == map || map.isEmpty();
        }
        public boolean matchOrBlank(Map<String,String> selector,Map<String,String> value) {
            return isBlank(selector) || matchesInput(selector, value, true);
        }
        public boolean matchOrBlank(String selector,String value) {
            return isBlank(selector) || matchesInput(selector, value);
        }

        public boolean matchOrBlank(String selector, Collection set) {
            return isBlank(selector) || matchesInputSet(selector, set);
        }

        public boolean matches(INodeEntry entry) {
            return !isBlank() && matchOrBlank(hostname, entry.getHostname()) &&
                   matchOrBlank(name, entry.getNodename()) &&
                   matchOrBlank(tags, entry.getTags()) &&
                   matchOrBlank(osfamily, entry.getOsFamily()) &&
                   matchOrBlank(osarch, entry.getOsArch()) &&
                   matchOrBlank(osname, entry.getOsName()) &&
                   matchOrBlank(osversion, entry.getOsVersion()) &&
                   matchOrBlank(getAttributesMap(), entry.getAttributes())
                ;
        }



        public String getOsfamily() {
            return osfamily;
        }

        public void setOsfamily(String osfamily) {
            this.osfamily = osfamily;
        }

        public String getOsarch() {
            return osarch;
        }

        public void setOsarch(String osarch) {
            this.osarch = osarch;
        }

        public String getOsname() {
            return osname;
        }

        public void setOsname(String osname) {
            this.osname = osname;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        public String getOsversion() {
            return osversion;
        }

        public void setOsversion(String osversion) {
            this.osversion = osversion;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public boolean isDominant() {
            return dominant;
        }

        public void setDominant(boolean dominant) {
            this.dominant = dominant;
        }



        /**
         * Generate a Map of attribute names to values
         * @return attributes
         */
        Map<String,String> getAttributesMap(){
            return attributesMap;
        }

        public void setAttributesMap(Map<String, String> attributesMap) {
            this.attributesMap = attributesMap;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof SetSelector)) {
                return false;
            }

            SetSelector that = (SetSelector) o;

            if (dominant != that.dominant) {
                return false;
            }
            if (attributesMap != null ? !attributesMap.equals(that.attributesMap) : that.attributesMap != null) {
                return false;
            }
            if (hostname != null ? !hostname.equals(that.hostname) : that.hostname != null) {
                return false;
            }
            if (name != null ? !name.equals(that.name) : that.name != null) {
                return false;
            }
            if (osarch != null ? !osarch.equals(that.osarch) : that.osarch != null) {
                return false;
            }
            if (osfamily != null ? !osfamily.equals(that.osfamily) : that.osfamily != null) {
                return false;
            }
            if (osname != null ? !osname.equals(that.osname) : that.osname != null) {
                return false;
            }
            if (osversion != null ? !osversion.equals(that.osversion) : that.osversion != null) {
                return false;
            }
            if (tags != null ? !tags.equals(that.tags) : that.tags != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = (dominant ? 1 : 0);
            result = 31 * result + (hostname != null ? hostname.hashCode() : 0);
            result = 31 * result + (osfamily != null ? osfamily.hashCode() : 0);
            result = 31 * result + (osarch != null ? osarch.hashCode() : 0);
            result = 31 * result + (osname != null ? osname.hashCode() : 0);
            result = 31 * result + (tags != null ? tags.hashCode() : 0);
            result = 31 * result + (osversion != null ? osversion.hashCode() : 0);
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (attributesMap != null ? attributesMap.hashCode() : 0);
            return result;
        }
    }

    public class Include extends SetSelector {

    }

    public class Exclude extends SetSelector {
    }
}
