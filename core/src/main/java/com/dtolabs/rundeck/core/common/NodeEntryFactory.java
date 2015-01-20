/*
 * Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
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

/*
* NodeEntryFactory.java
*
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Jan 20, 2011 11:58:42 AM
*
*/
package com.dtolabs.rundeck.core.common;

import org.apache.commons.beanutils.BeanUtils;

import java.util.*;
import java.lang.reflect.InvocationTargetException;

import com.dtolabs.shared.resources.ResourceXMLConstants;

/**
 * NodeEntryFactory creates NodeEntryImpls
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class NodeEntryFactory {

    /**
     * Properties to exclude when creating NodeEntryImpl from parsed input data
     */
    private static String[] excludeProps = {
        "attributes",
        "frameworkProject",
        "class"
    };

    /**
     * Create NodeEntryImpl from map data.  It will convert "tags" of type String as a comma separated list of tags, or
     * "tags" a collection of strings into a set.  It will remove properties excluded from allowed import.
     *
     * @param map input map data
     *
     * @return new entry
     *
     * @throws IllegalArgumentException if name is not set
     */
    @SuppressWarnings ("unchecked")
    public static NodeEntryImpl createFromMap(final Map<String, Object> map) throws IllegalArgumentException {
        final NodeEntryImpl nodeEntry = new NodeEntryImpl();
        final HashMap<String, Object> newmap = new HashMap<String, Object>(map);
        for (final String excludeProp : excludeProps) {
            newmap.remove(excludeProp);
        }
        if (null != newmap.get("tags") && newmap.get("tags") instanceof String) {
            String tags = (String) newmap.get("tags");
            String[] data;
            if ("".equals(tags.trim())) {
                data = new String[0];
            } else {
                data = tags.split(",");
            }
            final HashSet set = new HashSet();
            for (final String s : data) {
                if (null != s && !"".equals(s.trim())) {
                    set.add(s.trim());
                }
            }
            newmap.put("tags", set);
        } else if (null != newmap.get("tags") && newmap.get("tags") instanceof Collection) {
            Collection tags = (Collection) newmap.get("tags");
            HashSet data = new HashSet();
            for (final Object tag : tags) {
                if(null!=tag && !"".equals(tag.toString().trim())){
                    data.add(tag.toString().trim());
                }
            }
            newmap.put("tags", data);
        }else if (null != newmap.get("tags")) {
            Object o = newmap.get("tags");
            newmap.put("tags", new HashSet(Arrays.asList(o.toString().trim())));
        }
        try {
            BeanUtils.populate(nodeEntry, newmap);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        if (null == nodeEntry.getNodename()) {
            throw new IllegalArgumentException("Required property 'nodename' was not specified");
        }
        if (null == nodeEntry.getHostname()) {
            throw new IllegalArgumentException("Required property 'hostname' was not specified");
        }
        if (null == nodeEntry.getAttributes()) {
            nodeEntry.setAttributes(new HashMap<String, String>());
        }

        //populate attributes with any keys outside of nodeprops
        for (final Map.Entry<String, Object> entry : newmap.entrySet()) {
            if (!ResourceXMLConstants.allPropSet.contains(entry.getKey())) {
                Object value = entry.getValue();
                if (null != value) {
                    nodeEntry.setAttribute(entry.getKey(), value.toString());
                }
            }
        }

        return nodeEntry;
    }

    public static Map<String,String> toMap(final INodeEntry node) {
        HashMap<String, String> map = new HashMap<String, String>();
        if(null!=node.getAttributes()) {
            map.putAll(node.getAttributes());
        }

        if(null==map.get("tags")) {
            map.put("tags", "");
        }
        return map;
    }
}
