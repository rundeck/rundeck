

import grails.test.mixin.support.GrailsUnitTestMixin;
import groovy.xml.MarkupBuilder

import com.dtolabs.rundeck.app.support.BuilderUtil

import rundeck.CommandExec

/*
* Copyright 2011 DTO Labs, Inc. (http://dtolabs.com)
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
*
*/
 
/*
 * BuilderUtilTests.java
 * 
 * User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 * Created: Jan 25, 2011 10:43:14 AM
 * 
 */

@TestMixin(GrailsUnitTestMixin)
public class BuilderUtilTests{

    void testBasic(){
        def map = [a:'b',c:'d']
        final String string = assertObjToDom(map)
        assertEquals("<test><a>b</a><c>d</c></test>",string)
    }
    void testColl(){
        def map = [a:['b','c','d']]
        final String string = assertObjToDom(map)
        assertEquals("<test><a>b</a><a>c</a><a>d</a></test>",string)
    }
    void testColls(){
        def map = ['as':['b','c','d']]
        BuilderUtil.makePlural(map,'as')
        final String string = assertObjToDom(map)
        assertEquals("<test><as><a>b</a><a>c</a><a>d</a></as></test>",string)
    }
    void testMap(){
        def map = [a:[b:'c']]
        final String string = assertObjToDom(map)
        assertEquals("<test><a><b>c</b></a></test>",string)
    }
    void testMapText(){
        assertEquals("<test><a>balogna</a></test>", assertObjToDom([a:['<text>':'balogna']]))
    }
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
        System.err.println("string: ${string}");
        string
    }

    void testNull(){
        def map = [a:null]
        final String string = assertObjToDom(map)
        assertEquals("<test><a /></test>",string)
    }

    void testToMap(){
        def map = [a:new CommandExec([adhocLocalString:'test',argString:'blah'])]

        final String string = assertObjToDom(map)
        assertEquals("<test><a><script>test</script><args>blah</args></a></test>",string)
    }
    void testAttributes(){
        def map = [a:['f':'g']]
        BuilderUtil.addAttribute(map.a,'b','c')
        final String string = assertObjToDom(map)
        assertEquals("<test><a b='c'><f>g</f></a></test>",string)
    }
    void testShouldOutputCdata(){
        def map = ["a<cdata>":"data"]
        final String string = assertObjToDom(map)
        assertEquals("<test><a><![CDATA[data]]></a></test>",string)
    }
    void testShouldOutputUnescapedCdata(){
        def map = ["a<cdata>":"<monkey>donut</monkey>"]
        final String string = assertObjToDom(map)
        assertEquals("<test><a><![CDATA[<monkey>donut</monkey>]]></a></test>",string)
    }
    void testShouldOutputMultipleCdataIfNecessary(){
        def map = ["a<cdata>":"<monkey>donut]]></monkey>"]
        final String string = assertObjToDom(map)
        assertEquals("<test><a><![CDATA[<monkey>donut]]]]><![CDATA[></monkey>]]></a></test>",string)
    }


}
