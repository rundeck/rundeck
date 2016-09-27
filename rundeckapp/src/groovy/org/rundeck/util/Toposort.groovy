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

package org.rundeck.util

/**
 * Created by greg on 9/27/16.
 */
class Toposort {

    /**
     * Return topo sorted list of nodes, if acyclic, preserving
     * order of input node list for independent nodes
     * @param nodes list of nodes
     * @param oedgesin outbound edges, map of node -> [outbound node,..] (list)
     * @param iedgesin inbound edges, map of node -> [inbound node,..] list
     * @return [result: List] if acyclic or [cycle: Map] if cyclic
     */
    static Map toposort(List nodes, Map oedgesin, Map iedgesin) {
        def Map oedges = deepClone(oedgesin)
        def Map iedges = deepClone(iedgesin)
        def l = new ArrayList()
        List s = new ArrayList(nodes.findAll { !iedges[it] })
        while (s) {
            def n = s.first()
            s.remove(n)
            l.add(n)
            //for each node dependent on n
            def edges = new ArrayList()
            if (oedges[n]) {
                edges.addAll(oedges[n])
            }
            def k = [] //preserve input order when processing new leaf nodes
            edges.each { p ->
                oedges[n].remove(p)
                iedges[p].remove(n)
                if (!iedges[p]) {
                    k << p
                }
            }
            if (k) {
                s.addAll(0, k)
            }
        }
        if (iedges.any { it.value } || oedges.any { it.value }) {
            //cyclic graph
            return [cycle: iedges]
        } else {
            return [result: l]
        }
    }

    static deepClone(Map map) {
        def copy = [:]
        map.each { k, v ->
            if (v instanceof List) {
                copy[k] = v.clone()
            } else {
                copy[k] = v
            }
        }
        return copy
    }
}
