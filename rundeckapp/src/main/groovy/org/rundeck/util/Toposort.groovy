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

import groovy.transform.CompileStatic

import java.util.function.Function

/**
 * Created by greg on 9/27/16.
 */
@CompileStatic
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
        return toposort(nodes, oedgesin.&get, iedgesin.&get)
    }
    /**
     * Return topo sorted list of nodes, if acyclic, preserving
     * order of input node list for independent nodes
     * @param nodes list of nodes
     * @param oedgesin outbound edges, map of node -> [outbound node,..] (list)
     * @param iedgesin inbound edges, map of node -> [inbound node,..] list
     * @return [result: List] if acyclic or [cycle: Map] if cyclic
     */
    static Map toposort(List nodes,
                        Function<Object, List<Object>> outbound,
                        Function<Object, List<Object>> inbound) {
        if (!nodes) {
            return [result: nodes]
        }
        Map<Object,List> oedges = [:]
        Map<Object,List> iedges = [:]
        nodes.each { oedges[it] = new ArrayList<>(outbound.apply(it) ?: []) }
        nodes.each { iedges[it] = new ArrayList<>(inbound.apply(it) ?: []) }
        //unify outbound/inbound
        oedges.each{k,v->
            v.each{x->
                if(!iedges[x]){
                    iedges[x]=[k]
                }else if(!iedges[x].contains(k)){
                    iedges[x]<<k
                }
            }
        }
        iedges.each{k,v->
            v.each{x->
                if(!oedges[x]){
                    oedges[x]=[k]
                }else if(!oedges[x].contains(k)){
                    oedges[x]<<k
                }
            }
        }
        def l = new ArrayList()
        List s = nodes.findAll { !iedges[it] }
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
}
