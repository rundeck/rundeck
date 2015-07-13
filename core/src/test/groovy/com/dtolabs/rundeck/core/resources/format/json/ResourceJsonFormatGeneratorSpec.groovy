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
