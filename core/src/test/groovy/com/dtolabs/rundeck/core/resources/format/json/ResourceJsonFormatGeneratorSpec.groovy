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

package com.dtolabs.rundeck.core.resources.format.json

import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification

/**
 * Created by greg on 5/22/15.
 */
class ResourceJsonFormatGeneratorSpec extends Specification {
    def "empty"(){
        given:
        def gen = new ResourceJsonFormatGenerator()
        def baos = new ByteArrayOutputStream()
        def nodes=new NodeSetImpl()
        gen.generateDocument(nodes, baos)
        expect:
        baos.toString()=='{}'
    }
    def "basic"(){
        given:
        def json = new ObjectMapper()
        def gen = new ResourceJsonFormatGenerator()
        def baos = new ByteArrayOutputStream()
        def nodes=new NodeSetImpl()
        def node1 = new NodeEntryImpl("test1")
        node1.setAttribute("abc","123")
        nodes.putNode(node1)
        gen.generateDocument(nodes, baos)
        def result=json.readValue(baos.toString(),Map)
        expect:
        result instanceof Map
        result.size()==1
        result['test1']!=null
        result['test1'] instanceof Map
        result['test1']==["abc":"123","nodename":"test1"]
    }
    def "tags"(){
        given:
        def json = new ObjectMapper()
        def gen = new ResourceJsonFormatGenerator()
        def baos = new ByteArrayOutputStream()
        def nodes=new NodeSetImpl()
        def node1 = new NodeEntryImpl("test1")
        node1.setAttribute("abc","123")
        node1.setTags(["xyz","456"] as Set)
        nodes.putNode(node1)
        gen.generateDocument(nodes, baos)
        def result=json.readValue(baos.toString(),Map)

        expect:
        result instanceof Map
        result.size()==1
        result['test1']!=null
        result['test1'] instanceof Map
        result['test1']==["tags":"456, xyz","abc":"123","nodename":"test1"]
    }
}
