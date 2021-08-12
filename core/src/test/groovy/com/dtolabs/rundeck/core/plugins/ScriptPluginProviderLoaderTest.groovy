/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.core.plugins

import com.dtolabs.rundeck.core.plugins.metadata.PluginMeta
import com.dtolabs.rundeck.core.plugins.metadata.ProviderDef
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.error.YAMLException
import spock.lang.Specification
import spock.lang.Unroll

import static com.dtolabs.rundeck.core.plugins.PluginValidation.State.INCOMPATIBLE
import static com.dtolabs.rundeck.core.plugins.PluginValidation.State.INVALID
import static com.dtolabs.rundeck.core.plugins.PluginValidation.State.VALID

class ScriptPluginProviderLoaderTest extends Specification {

    def "LoadMetadataYaml with null date"() {
        setup:
        File tmpCacheDir = File.createTempDir()

        when:
        String pluginName = "Test script plugin"
        ScriptPluginProviderLoader loader = new ScriptPluginProviderLoader(File.createTempFile("throwaway","unneeded"),tmpCacheDir)
        def pluginYaml = createPluginYaml(pluginName,"1.2")
        def meta = loader.loadMetadataYaml(pluginYaml)
        def dte = loader.getPluginDate()

        then:
        noExceptionThrown()
        meta.name == pluginName
        meta.rundeckPluginVersion == "1.2"
        dte == null
    }

    def "LoadMetadataYaml"() {
        setup:
            File tmpCacheDir = File.createTempDir()

            String pluginName = "Test script plugin"
            ScriptPluginProviderLoader loader = new ScriptPluginProviderLoader(File.createTempFile("throwaway","unneeded"),tmpCacheDir)
            def pluginYaml = createPluginYaml(pluginName,"1.2",[
                author:'author',
                date:'adate',
                version:'aversion',
                url:'aurl',
                resourcesDir:'adir',
                resourcesList:['res1','res2'],
                providers:[
                    [
                        'name':'aprov',
                        'service':'aservice',
                        'script-file':'afile',
                        'script-args':'args',
                        'script-interpreter':'interp',
                        'interpreter-args-quoted':true,
                        'plugin-type':'type',
                        'plugin-meta':[
                            'meta1':'metaA',
                            'meta2':'metaB'
                        ],
                    ]
                ],
            ])
        when: "load plugin yaml"
            def meta = loader.loadMetadataYaml(pluginYaml)

        then: "expected values are defined"
            meta.name == pluginName
            meta.rundeckPluginVersion == "1.2"
            meta.date == 'adate'
            meta.version == 'aversion'
            meta.url == 'aurl'
            meta.resourcesDir == 'adir'
            meta.resourcesList == ['res1','res2']
            meta.providers.size()==1
            meta.pluginDefs.size()==1
            meta.pluginDefs[0] instanceof ProviderDef
            meta.pluginDefs[0].name=='aprov'
            meta.pluginDefs[0].service=='aservice'
            meta.pluginDefs[0].scriptFile=='afile'
            meta.pluginDefs[0].scriptArgs=='args'
            meta.pluginDefs[0].scriptInterpreter=='interp'
            meta.pluginDefs[0].interpreterArgsQuoted==true
            meta.pluginDefs[0].pluginType=='type'
            meta.pluginDefs[0].providerMeta==[
                'meta1':'metaA',
                'meta2':'metaB'
            ]
    }
    def "LoadMetadataYaml args array"() {
        setup:
        File tmpCacheDir = File.createTempDir()
            String pluginName = "Test script plugin"
            ScriptPluginProviderLoader loader = new ScriptPluginProviderLoader(File.createTempFile("throwaway","unneeded"),tmpCacheDir)
            def pluginYaml = createPluginYaml(pluginName,"1.2",[
                author:'author',
                date:'adate',
                version:'aversion',
                url:'aurl',
                resourcesDir:'adir',
                resourcesList:['res1','res2'],
                providers:[
                    [
                        'name':'aprov',
                        'service':'aservice',
                        'script-file':'afile',
                        'script-args':[
                            'args1',
                            'args2'
                        ],
                        'script-interpreter':'interp',
                        'interpreter-args-quoted':true,
                        'plugin-type':'type',
                        'plugin-meta':[
                            'meta1':'metaA',
                            'meta2':'metaB'
                        ],
                    ]
                ],
            ])
        when: "yaml has script-args with a sequence value"
        def meta = loader.loadMetadataYaml(pluginYaml)

        then: "scriptArgsArray is set in the ProviderDef"
        meta.name == pluginName
        meta.rundeckPluginVersion == "1.2"
        meta.date == 'adate'
        meta.version == 'aversion'
        meta.url == 'aurl'
        meta.resourcesDir == 'adir'
        meta.resourcesList == ['res1','res2']
        meta.providers.size()==1
        meta.pluginDefs.size()==1
        meta.pluginDefs[0] instanceof ProviderDef
        meta.pluginDefs[0].name=='aprov'
        meta.pluginDefs[0].service=='aservice'
        meta.pluginDefs[0].scriptFile=='afile'
        meta.pluginDefs[0].scriptArgs==null
        meta.pluginDefs[0].scriptArgsArray.toList()==['args1','args2']
        meta.pluginDefs[0].scriptInterpreter=='interp'
        meta.pluginDefs[0].interpreterArgsQuoted==true
        meta.pluginDefs[0].pluginType=='type'
        meta.pluginDefs[0].providerMeta==[
            'meta1':'metaA',
            'meta2':'metaB'
        ]
    }

    static final String TEST_YAML1='''name: plugin-name
version: 1.0
rundeckPluginVersion: 1.2
providers:
    - name: zingbat
      plugin-meta:
        test: !!java.lang.Object
      '''
    static final String TEST_YAML2='''name: plugin-name
version: 1.0
rundeckPluginVersion: 1.2
providers:
    - !!java.lang.Object
      '''
    static final String TEST_YAML3='''name: plugin-name
version: 1.0
rundeckPluginVersion: 1.2
resourcesList:
    - !!java.lang.Object
providers:
    - name: zingbat
      '''
    static final String TEST_YAML4='''name: plugin-name
version: 1.0
rundeckPluginVersion: 1.2
tags:
    - !!java.lang.Object
providers:
    - name: zingbat
      '''

    def "LoadMetadataYaml unsafe"() {
        setup:
            File tmpCacheDir = File.createTempDir()
            ScriptPluginProviderLoader loader = new ScriptPluginProviderLoader(File.createTempFile("throwaway","unneeded"),tmpCacheDir)
            def pluginYaml =  new ByteArrayInputStream(testYaml.bytes)
        when: "load yaml with java class tags"
            def meta = loader.loadMetadataYaml(pluginYaml)

        then: "should throw exception"
            YAMLException e = thrown()
            e.message.contains 'could not determine a constructor for the tag tag:yaml.org,2002:java.lang.Object'
        where:
            testYaml<<[
                TEST_YAML1,
                TEST_YAML2,
                TEST_YAML3,
                TEST_YAML4,
            ]
    }

    @Unroll
    def "ValidatePluginMeta"() {
        setup:
        File fakeFile = new File("/tmp/fake-plugin.yaml")
        when:

        def validation = ScriptPluginProviderLoader.validatePluginMeta(new PluginMeta(pluginMeta),fakeFile)

        then:
        logResult == validation.messages
        validation.state.valid == expectation
        validation.state == state

        where:
        expectation |state                                                                             | pluginMeta | logResult
        false       |INVALID| [rundeckPluginVersion: "1.2", pluginDefs: []] | ["'name' not found in metadata", "'version' not found in metadata"]
        false       |INVALID| [name:"Test script",rundeckPluginVersion: "1.2", pluginDefs: []]                | ["'version' not found in metadata"]
        true        |VALID| [name:"Test script",rundeckPluginVersion: "1.2", version:"1.0", pluginDefs: []] | []
        false       |INVALID| [name:"Test script",rundeckPluginVersion: "2.0", version:"1.0", pluginDefs: []] | ["No targetHostCompatibility property specified in metadata","rundeckCompatibilityVersion cannot be null in metadata"]
        false       |INVALID| [name:"Test script",rundeckPluginVersion: "2.0", version:"1.0", targetHostCompatibility:"all", pluginDefs: []] | ["rundeckCompatibilityVersion cannot be null in metadata"]
        false       |INCOMPATIBLE| [name:"Test script",rundeckPluginVersion: "2.0", version:"1.0", targetHostCompatibility:"all", rundeckCompatibilityVersion:"1.0", pluginDefs: []] | ["Plugin is not compatible with this version of Rundeck"]
        true        |VALID| [name:"Test script",rundeckPluginVersion: "2.0", version:"1.0", targetHostCompatibility:"all", rundeckCompatibilityVersion:"3.x", pluginDefs: []] | []
        true        |VALID| [name:"test-script",display:"Test script",rundeckPluginVersion: "2.0", version:"1.0", targetHostCompatibility:"all", rundeckCompatibilityVersion:"3.x", pluginDefs: []] | []

    }


    /**
     * create yaml stream
     * @param pluginName `name` value
     * @param pluginVersion `rundeckPluginVersion` value
     * @param props additional yaml structure
     * @return inputstream of yaml string representing the structure
     */
    ByteArrayInputStream createPluginYaml(String pluginName, String pluginVersion, Map props = [:]) {
        Yaml yaml = new Yaml()
        def map = [name:pluginName,rundeckPluginVersion: pluginVersion] + props
        def writer=new StringWriter()
        yaml.dump(map,writer)
        new ByteArrayInputStream(writer.toString().bytes)
    }
}
