/*
 * Copyright 2014 SimplifyOps Inc, <http://simplifyops.com>
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

package com.dtolabs.rundeck.plugin.stub;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.common.NodeEntryImpl;
import com.dtolabs.rundeck.core.common.NodeSetImpl;
import com.dtolabs.rundeck.core.resources.ResourceModelSource;
import com.dtolabs.rundeck.core.resources.ResourceModelSourceException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * StubResourceModelSource is ...
 *
 * @author Greg Schueler <a href="mailto:greg@simplifyops.com">greg@simplifyops.com</a>
 * @since 2014-09-03
 */
public class StubResourceModelSource implements ResourceModelSource {
    Properties configuration;
    int count;
    String prefix;
    String suffix;
    String tags;
    Map<String, String> attrs;
    int delayMin, delayMax;

    public StubResourceModelSource(Properties configuration) {
        this.configuration = configuration;
        count = Integer.parseInt(configuration.getProperty("count"));
        prefix = configuration.getProperty("prefix");
        suffix = configuration.getProperty("suffix");
        tags = configuration.getProperty("tags");
        attrs = attrsMap(configuration.getProperty("attrs"));
        delayMin = minDelay(configuration.getProperty("delay"));
        delayMax = maxDelay(configuration.getProperty("delay"));
    }

    Pattern pattern = Pattern.compile("^(\\d+)(-(\\d+))?$");
    private int maxDelay(final String delay) {
        int[] matches = parseDelay(delay);
        return matches[1];
    }

    private int[] parseDelay(final String delay) {
        int[] results = new int[2];
        results[0] = -1;
        results[1] = -1;
        if(delay==null){
            return results;
        }

        Matcher matcher = pattern.matcher(delay);
        if (matcher.matches()) {
            String g0 = matcher.group(1);
            String g3 = matcher.group(3);
            if(null!=g0){
                try{
                    results[0] = Integer.parseInt(g0);
                }catch (NumberFormatException e){

                }
            }
            if(null!=g3){
                try{
                    results[1] = Integer.parseInt(g3);
                }catch (NumberFormatException e){

                }
            }
        }
        return results;
    }

    private int minDelay(final String delay) {
        int[] matches = parseDelay(delay);
        return matches[0];
    }

    private Map<String, String> attrsMap(final String attrs) {
        Map<String, String> result = new HashMap<>();
        if(null!=attrs) {
            for (String x : attrs.split(",")) {
                String[] vals = x.split("=", 2);
                if (vals.length == 2 && !"".equals(vals[0]) && !"".equals(vals[1])) {
                    result.put(vals[0], vals[1]);
                }
            }
        }
        return result;
    }

    @Override
    public INodeSet getNodes() throws ResourceModelSourceException {
        if(delayMin>=0){
            int delay;
            if (delayMax > delayMin) {
                delay = (int) Math.floor(delayMin + Math.random() * (delayMax - delayMin));
            } else {
                delay = delayMin;
            }
            if(delay>0) {
                try {
                    Thread.sleep(delay * 1000);
                } catch (InterruptedException e) {

                }
            }
        }
        NodeSetImpl iNodeEntries = new NodeSetImpl();
        for (int i = 0; i < count; i++) {
            NodeEntryImpl nodeEntry = new NodeEntryImpl();
            nodeEntry.setNodename((prefix != null ? prefix : "node") + "-" + i + (suffix != null ? suffix : ""));
            nodeEntry.setHostname((prefix != null ? prefix : "") + "host" + (suffix != null ? suffix : ""));
            nodeEntry.setUsername((prefix != null ? prefix : "") + "user" + (suffix != null ? suffix : ""));
            nodeEntry.setAttribute("node-executor", "stub");
            nodeEntry.setAttribute("file-copier", "stub");
            if (null != tags) {
                nodeEntry.setTags(new HashSet(Arrays.asList(tags.split("\\s*,\\s*"))));
            }
            if(null!=attrs && attrs.size()>0){
                nodeEntry.getAttributes().putAll(attrs);
            }

            iNodeEntries.putNode(nodeEntry);
        }
        return iNodeEntries;
    }
}
