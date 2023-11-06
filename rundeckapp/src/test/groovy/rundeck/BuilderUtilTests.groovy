package rundeck

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

import grails.test.mixin.TestMixin
import grails.test.mixin.support.GrailsUnitTestMixin;
import groovy.xml.MarkupBuilder

import com.dtolabs.rundeck.app.support.BuilderUtil
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import rundeck.CommandExec
import spock.lang.Specification

import static org.junit.Assert.*

/*
 * rundeck.BuilderUtilTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Jan 25, 2011 10:43:14 AM
 * 
 */
class BuilderUtilTests {

    @Test
    void testBasic(){
        def map = [a:'b',c:'d']
        final String string = assertObjToDom(map)
        assertEquals("<test><a>b</a><c>d</c></test>",string)
    }
    @Test
    void testColl(){
        def map = [a:['b','c','d']]
        final String string = assertObjToDom(map)
        assertEquals("<test><a>b</a><a>c</a><a>d</a></test>",string)
    }
    @Test
    void testColls(){
        def map = ['as':['b','c','d']]
        BuilderUtil.makePlural(map,'as')
        final String string = assertObjToDom(map)
        assertEquals("<test><as><a>b</a><a>c</a><a>d</a></as></test>",string)
    }
    @Test
    void testMap(){
        def map = [a:[b:'c']]
        final String string = assertObjToDom(map)
        assertEquals("<test><a><b>c</b></a></test>",string)
    }
    @Test
    void testMapText(){
        assertEquals("<test><a>balogna</a></test>", assertObjToDom([a:['<text>':'balogna']]))
    }
    @Test
    void testMapTextWithAttributes(){
        def map = [a: ['<text>': 'balogna']]
        BuilderUtil.addAttribute(map.a, 'b', 'c')
        assertEquals("<test><a b='c'>balogna</a></test>", assertObjToDom(map))
    }

    private String assertObjToDom(LinkedHashMap<String, LinkedHashMap<String, String>> map) {
        final StringWriter writer = new StringWriter()
        def builder = new MarkupBuilder(new IndentPrinter(new PrintWriter(writer), "", false))
        def bu = new BuilderUtil()
        bu.objToDom('test', map, builder)
        final String string = writer.toString()
        assertNotNull(string)
        string
    }

    @Test
    void testNull(){
        def map = [a:null]
        final String string = assertObjToDom(map)
        assertEquals("<test><a /></test>",string)
    }

    @Test
    void testToMap(){
        def map = [a:new CommandExec([adhocLocalString:'test',argString:'blah'])]

        final String string = assertObjToDom(map)
        assertEquals("<test><a><script>test</script><args>blah</args><enabled>true</enabled></a></test>",string)
    }
    @Test
    void testAttributes(){
        def map = [a:['f':'g']]
        BuilderUtil.addAttribute(map.a,'b','c')
        final String string = assertObjToDom(map)
        assertEquals("<test><a b='c'><f>g</f></a></test>",string)
    }
    @Test
    void testShouldOutputCdata(){
        def map = ["a<cdata>":"data"]
        final String string = assertObjToDom(map)
        assertEquals("<test><a><![CDATA[data]]></a></test>",string)
    }
    @Test
    void testShouldOutputUnescapedCdata(){
        def map = ["a<cdata>":"<monkey>donut</monkey>"]
        final String string = assertObjToDom(map)
        assertEquals("<test><a><![CDATA[<monkey>donut</monkey>]]></a></test>",string)
    }
    @Test
    void testShouldOutputMultipleCdataIfNecessary(){
        def map = ["a<cdata>":"<monkey>donut]]></monkey>"]
        final String string = assertObjToDom(map)
        assertEquals("<test><a><![CDATA[<monkey>donut]]]]><![CDATA[></monkey>]]></a></test>",string)
    }


}
