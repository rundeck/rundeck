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

package rundeck.services

import com.dtolabs.rundeck.core.authorization.RuleEvaluator
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.utils.PropertyLookup
import com.google.common.cache.Cache
import com.google.common.cache.LoadingCache
import grails.test.mixin.Mock
import grails.test.mixin.TestFor
import org.apache.commons.fileupload.util.Streams
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import rundeck.Project
import spock.lang.Specification
import spock.lang.Unroll

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ProjectManagerService)
@Mock([Project])
class ProjectManagerServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }

    void "exists project does not exist"(){
        when:
        def result=service.existsFrameworkProject('test1')

        then:
        !result
    }
    void "exists project does exist"(){
        setup:
        def p = new Project(name:'test1')
        p.save(flush: true)

        when:
        def result=service.existsFrameworkProject('test1')

        then:
        result
    }
    void "get project does not exist"(){
        when:
        def result=service.getFrameworkProject('test1')

        then:
        IllegalArgumentException e=thrown()
        e.message.contains('Project does not exist')
    }
    void "get project exists no props"(){
        setup:
        def p = new Project(name:'test1')
        p.save(flush: true)
        service.storage=Stub(StorageTree){

        }

        def properties = new Properties()
        properties.setProperty("fwkprop","fwkvalue")

        service.frameworkService=Stub(FrameworkService){
            getRundeckFramework() >> Stub(Framework){
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
        }
        service.rundeckNodeService=Mock(NodeService)
        when:
        def result=service.getFrameworkProject('test1')

        then:

        0*service.rundeckNodeService.getNodes('test1')
        result!=null
        'test1'==result.name
        'fwkvalue'==result.getProperty('fwkprop')
        'test1'==result.getProperty('project.name')
        1==result.getProjectProperties().size()
        'test1'==result.getProjectProperties().get('project.name')
        null==result.info.description
        null==result.info.readme
        null==result.info.motd
    }
    void "get project exists with props"(){
        setup:
        def description = 'blah'
        def p = new Project(name:'test1', description: description)
        p.save(flush: true)
        def modDate= new Date(123)

        service.storage=Stub(StorageTree){
            hasResource("projects/test1/etc/project.properties") >> true
            getResource("projects/test1/etc/project.properties") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nprojkey=projval\nproject.description='+description).bytes)
                    getModificationTime() >> modDate
                }
            }
        }

        def properties = new Properties()
        properties.setProperty("fwkprop","fwkvalue")

        service.frameworkService=Stub(FrameworkService){
            getRundeckFramework() >> Stub(Framework){
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
        }
        service.rundeckNodeService=Mock(NodeService)
        when:
        def result=service.getFrameworkProject('test1')

        then:
        0*service.rundeckNodeService.getNodes('test1')
        result!=null
        'test1'==result.name
        'fwkvalue'==result.getProperty('fwkprop')
        'test1'==result.getProperty('project.name')
        'projval'==result.getProperty('projkey')
        3==result.getProjectProperties().size()
        'test1'==result.getProjectProperties().get('project.name')
        description==result.getProjectProperties().get('project.description')
        'projval'==result.getProjectProperties().get('projkey')
        modDate==result.getConfigLastModifiedTime()
        'blah'==result.info.description
        null==result.info.readme
        null==result.info.motd
    }
    void "get project exists with readme/motd"(){
        setup:
        def description = 'blah'
        def p = new Project(name:'test1', description: description)
        p.save(flush: true)
        def modDate= new Date(123)

        service.storage=Mock(StorageTree){
            1*hasResource("projects/test1/etc/project.properties") >> true
            1*hasResource("projects/test1/readme.md") >> true
            1*hasResource("projects/test1/motd.md") >> true
            getResource("projects/test1/etc/project.properties") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nprojkey=projval\nproject.description='+description).bytes)
                    getModificationTime() >> modDate
                }
            }
            1*getResource("projects/test1/readme.md") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('blah readme'.bytes)
                    getModificationTime() >> modDate
                }
            }
            1*getResource("projects/test1/motd.md") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('blah motd'.bytes)
                    getModificationTime() >> modDate
                }
            }
        }

        def properties = new Properties()
        properties.setProperty("fwkprop","fwkvalue")

        service.frameworkService=Stub(FrameworkService){
            getRundeckFramework() >> Stub(Framework){
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
        }
        service.rundeckNodeService=Mock(NodeService)
        when:
        def result=service.getFrameworkProject('test1')

        then:
        0*service.rundeckNodeService.getNodes('test1')
        result!=null
        'test1'==result.name
        'fwkvalue'==result.getProperty('fwkprop')
        'test1'==result.getProperty('project.name')
        'projval'==result.getProperty('projkey')
        3==result.getProjectProperties().size()
        'test1'==result.getProjectProperties().get('project.name')
        description==result.getProjectProperties().get('project.description')
        'projval'==result.getProjectProperties().get('projkey')
        modDate==result.getConfigLastModifiedTime()
        'blah'==result.info.description
        'blah readme'==result.info.readme
        'blah motd'==result.info.motd
    }
    void "get project exists invalid props content"(){
        setup:
        def p = new Project(name:'test1')
        p.save(flush: true)
        def modDate= new Date(123)

        service.storage=Stub(StorageTree){
            hasResource("projects/test1/etc/project.properties") >> true
            getResource("projects/test1/etc/project.properties") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('#invalidcontent\nprojkey=projval'.bytes)
                    getModificationTime() >> modDate
                }
            }
        }

        def properties = new Properties()
        properties.setProperty("fwkprop","fwkvalue")

        service.frameworkService=Stub(FrameworkService){
            getRundeckFramework() >> Stub(Framework){
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
        }
        service.rundeckNodeService=Mock(NodeService)
        when:
        def result=service.getFrameworkProject('test1')

        then:
        0*service.rundeckNodeService.getNodes('test1')
        result!=null
        'test1'==result.name
        'fwkvalue'==result.getProperty('fwkprop')
        'test1'==result.getProperty('project.name')
        !result.hasProperty('projkey')
        1==result.getProjectProperties().size()
        'test1'==result.getProjectProperties().get('project.name')
        null==result.getProjectProperties().get('projkey')
        modDate==result.getConfigLastModifiedTime()
    }

    void "create project with props"(){
        setup:

        def props = new Properties()
        props['abc']='def'

        service.storage=Stub(StorageTree){
            hasResource("projects/test1/etc/project.properties") >> false
            createResource("projects/test1/etc/project.properties",{ResourceMeta rm->
                def tprops=new Properties()
                tprops.load(rm.inputStream)
                rm.meta.size()==1 && rm.meta[StorageUtil.RES_META_RUNDECK_CONTENT_TYPE]==ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES && tprops['abc']=='def'
            }) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nabc=def').bytes)
                }
            }
        }

        def properties = new Properties()
        properties.setProperty("fwkprop","fwkvalue")

        service.frameworkService=Stub(FrameworkService){
            getRundeckFramework() >> Stub(Framework){
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
        }
        service.rundeckNodeService=Mock(NodeService)
        service.projectCache=Mock(LoadingCache)

        when:

        def result = service.createFrameworkProject('test1',props)

        then:
        1*service.projectCache.invalidate('test1')
        1*service.rundeckNodeService.refreshProjectNodes('test1')
        0*service.rundeckNodeService.getNodes('test1')

        result.name=='test1'
        (2+ProjectManagerService.DEFAULT_PROJ_PROPS.size())==result.getProjectProperties().size()
        'test1'==result.getProjectProperties().get('project.name')
        'def'==result.getProjectProperties().get('abc')

        null!=Project.findByName('test1')
    }

    void "create project strict already exists"(){
        setup:
        Project p = new Project(name:'test1').save()
        def props = new Properties()
        props['abc']='def'


        when:

        service.createFrameworkProjectStrict('test1',props)

        then:

        IllegalArgumentException e = thrown()
        e.message.contains("project exists")
    }

    void "create project strict with props"(){
        setup:

        def props = new Properties()
        props['abc']='def'

        service.storage=Stub(StorageTree){
            hasResource("projects/test1/etc/project.properties") >> false
            createResource("projects/test1/etc/project.properties",{ResourceMeta rm->
                def tprops=new Properties()
                tprops.load(rm.inputStream)
                rm.meta.size()==1 && rm.meta[StorageUtil.RES_META_RUNDECK_CONTENT_TYPE]==ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES && tprops['abc']=='def'
            }) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nabc=def').bytes)
                }
            }
        }

        def properties = new Properties()
        properties.setProperty("fwkprop","fwkvalue")

        service.frameworkService=Stub(FrameworkService){
            getRundeckFramework() >> Stub(Framework){
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
        }

        service.rundeckNodeService=Mock(NodeService)
        service.projectCache=Mock(LoadingCache)
        when:

        def result = service.createFrameworkProjectStrict('test1',props)

        then:
        1*service.projectCache.invalidate('test1')
        1*service.rundeckNodeService.refreshProjectNodes('test1')
        0*service.rundeckNodeService.getNodes('test1')

        0*service.rundeckNodeService._(*_)
        result.name=='test1'
        (2+ProjectManagerService.DEFAULT_PROJ_PROPS.size())==result.getProjectProperties().size()
        'test1'==result.getProjectProperties().get('project.name')
        'def'==result.getProjectProperties().get('abc')

        null!=Project.findByName('test1')
    }

    void "remove project does not exist"(){

        when:

        service.removeFrameworkProject('test1')

        then:
        0*service.rundeckNodeService._(*_)
        IllegalArgumentException e = thrown()
        e.message.contains('does not exist')
    }

    void "remove project does exist"(){
        setup:
        def p = new Project(name:'test1')
        p.save(flush: true)

        service.storage=Mock(StorageTree){
            1*hasResource({it.path=="projects/test1"}) >> false
            1*hasDirectory({it.path=="projects/test1"}) >> true
            1*listDirectory({it.path=="projects/test1"}) >> []
        }
        service.rundeckNodeService=Mock(NodeService)
        service.projectCache=Mock(LoadingCache)
        when:

        service.removeFrameworkProject('test1')

        then:
        1*service.projectCache.invalidate('test1')
        1*service.rundeckNodeService.refreshProjectNodes('test1')
        null==Project.findByName('test1')

    }

    void "valid config file"(String data, boolean valid){
        expect:
        valid==service.isValidConfigFile(data.bytes)

        where:
        data                                                                                 | valid
        'abc=123'                                                                            | false
        '#somethingelse\nabc=123'                                                            | false
        '#' + ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES + '\nabc=123'               | true
        '#' + ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES + ';project=test1\nabc=123' | true
        '#text/x-java-propertie'                                                             | false
    }

    void "merge project properties internal"(){
        setup:
        Properties props1 = new Properties()
        props1['def']='ghi'
        new Project(name:'test1').save()
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/etc/project.properties") >> true
            getResource("projects/test1/etc/project.properties") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nabc=def').bytes)
                }
            }
            updateResource("projects/test1/etc/project.properties",{ResourceMeta rm->
                def tprops=new Properties()
                tprops.load(rm.inputStream)
                rm.meta.size()==1 && rm.meta[StorageUtil.RES_META_RUNDECK_CONTENT_TYPE]==ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES && tprops['abc']=='def' && tprops['def']=='ghi'
            }) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nabc=def\ndef=ghi').bytes)
                }
            }
        }

        service.rundeckNodeService=Mock(NodeService)
        service.projectCache=Mock(LoadingCache)

        when:
        def res=service.mergeProjectProperties('test1',props1,[] as Set)

        then:
        1*service.projectCache.invalidate('test1')
        1*service.rundeckNodeService.refreshProjectNodes('test1')

        res != null
        res.config.size() == 3
        'def' == res.config['abc']
        'ghi' == res.config['def']
        'test1' == res.config['project.name']
    }

    @Unroll
    void "merge project properties without existing resource"() {
        setup:
        Properties props1 = new Properties()
        props1['def'] = 'ghi'
        new Project(name: 'test1').save()
        service.storage = Stub(StorageTree) {
            hasResource("projects/test1/etc/project.properties") >> false
            updateResource(
                "projects/test1/etc/project.properties", { ResourceMeta rm ->
                def tprops = new Properties()
                tprops.load(rm.inputStream)
                rm.meta.size() == 1 &&
                rm.meta[StorageUtil.RES_META_RUNDECK_CONTENT_TYPE] ==
                ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES &&
                tprops['def'] ==
                'ghi'
            }
            ) >> Stub(Resource) {
                getContents() >> Stub(ResourceMeta) {
                    getInputStream() >> new ByteArrayInputStream(
                        (
                            '#' + ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES +
                            '\ndef=ghi'
                        ).bytes
                    )
                }
            }
        }

        service.rundeckNodeService = Mock(NodeService)
        service.projectCache = Mock(LoadingCache)

        when:
        def res = service.mergeProjectProperties('test1', props1, removePrefixes as Set)

        then:
        1 * service.projectCache.invalidate('test1')
        1 * service.rundeckNodeService.refreshProjectNodes('test1')

        res != null
        res.config.size() == 2
        'ghi' == res.config['def']
        'test1' == res.config['project.name']

        where:
        removePrefixes       | _
        ['resources.source'] | _
        []                   | _
    }
    void "set project properties internal"(){
        setup:
        Properties props1 = new Properties()
        props1['def']='ghi'
        new Project(name:'test1').save(flush: true)
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/etc/project.properties") >> true
            getResource("projects/test1/etc/project.properties") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nabc=def').bytes)
                }
            }
            updateResource("projects/test1/etc/project.properties",{ResourceMeta rm->
                def tprops=new Properties()
                tprops.load(rm.inputStream)
                rm.meta.size()==1 && rm.meta[StorageUtil.RES_META_RUNDECK_CONTENT_TYPE]==ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES && tprops['abc']==null && tprops['def']=='ghi'
            }) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\ndef=ghi').bytes)
                }
            }
        }

        service.rundeckNodeService=Mock(NodeService)
        service.projectCache=Mock(LoadingCache)

        when:
        def res=service.setProjectProperties('test1',props1)

        then:

        1*service.projectCache.invalidate('test1')
        1*service.rundeckNodeService.refreshProjectNodes('test1')
        res!=null
        res.config.size()==2
        null==res.config['abc']
        'ghi'==res.config['def']
        'test1'==res.config['project.name']
    }
    void "set project properties should not change project.name"(){
        setup:
        Properties props1 = new Properties()
        props1['def']='ghi'
        props1['project.name']='not-right'
        new Project(name:'test1').save(flush: true)
        service.storage=Mock(StorageTree){
            1 * hasResource("projects/test1/etc/project.properties") >> true
            1 * updateResource("projects/test1/etc/project.properties",{ResourceMeta rm->
                def tprops=new Properties()
                tprops.load(rm.inputStream)
                rm.meta.size()==1 &&
                        rm.meta[StorageUtil.RES_META_RUNDECK_CONTENT_TYPE]==ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES &&
                        tprops['abc']==null &&
                        tprops['def']=='ghi' &&
                        tprops['project.name']=='test1'
            }) >> Mock(Resource){
                2 * getContents()>> Mock(ResourceMeta){
                    1 * getModificationTime()
                    1 * getCreationTime()
                }
            }
            0 * _(*_)
        }


        service.rundeckNodeService=Mock(NodeService)
        service.projectCache=Mock(LoadingCache)
        when:
        def res=service.setProjectProperties('test1',props1)

        then:

        1*service.projectCache.invalidate('test1')
        1*service.rundeckNodeService.refreshProjectNodes('test1')
        res!=null
        res.config.size()==2
        null==res.config['abc']
        res.config['def']=='ghi'
        res.config['project.name']=='test1'
    }

    void "merge properties no conflict"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("def","456")
        Properties newprops=new Properties()
        newprops.setProperty("ghi","789")
        Set<String> removePrefixes=[]

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        3==result.size()
        "123"==result.getProperty("abc")
        "456"==result.getProperty("def")
        "789"==result.getProperty("ghi")

    }

    void "merge properties override"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("def","456")
        Properties newprops=new Properties()
        newprops.setProperty("abc","789")
        Set<String> removePrefixes=[]

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        2==result.size()
        "789"==result.getProperty("abc")
        "456"==result.getProperty("def")

    }

    void "merge properties remove prefix"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("def","456")
        Properties newprops=new Properties()
        newprops.setProperty("ghi","789")
        Set<String> removePrefixes=['de']

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        2==result.size()
        "123"==result.getProperty("abc")
        null==result.getProperty("def")
        "789"==result.getProperty("ghi")

    }

    void "merge properties remove prefix multiple hits"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("def","456")
        oldprops.setProperty("defleopard","money")
        Properties newprops=new Properties()
        newprops.setProperty("ghi","789")
        Set<String> removePrefixes=['de']

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        2==result.size()
        "123"==result.getProperty("abc")
        null==result.getProperty("def")
        null==result.getProperty("defleopard")
        "789"==result.getProperty("ghi")
    }

    void "merge properties remove multiple prefixes multiple hits"() {
        given:
        Properties oldprops=new Properties()
        oldprops.setProperty("abc","123")
        oldprops.setProperty("abcdef","488")
        oldprops.setProperty("def","456")
        oldprops.setProperty("defleopard","money")
        Properties newprops=new Properties()
        newprops.setProperty("ghi","789")
        Set<String> removePrefixes=['de','ab']

        when:
        Properties result=ProjectManagerService.mergeProperties(removePrefixes,oldprops,newprops)

        then:
        1==result.size()
        null==result.getProperty("abc")
        null==result.getProperty("abcdef")
        null==result.getProperty("def")
        null==result.getProperty("defleopard")
        "789"==result.getProperty("ghi")
    }

    void "storage exists test"(){
        given:
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/my-resource") >> true
            hasResource("projects/test1/not-my-resource") >> false
        }
        expect:
        service.existsProjectFileResource("test1","my-resource")
        !service.existsProjectFileResource("test1","not-my-resource")
    }
    void "storage dir exists test"(){
        given:
        service.storage=Stub(StorageTree){
            hasDirectory("projects/test1/my-resource") >> true
            hasDirectory("projects/test1/not-my-resource") >> false
        }
        expect:
        service.existsProjectDirResource("test1","my-resource")
        !service.existsProjectDirResource("test1","not-my-resource")
    }
    void "storage list paths"(){
        given:
        service.storage=Stub(StorageTree){
            hasDirectory("projects/test1/my-dir") >> true
            listDirectory("projects/test1/my-dir") >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("projects/test1/my-dir/file1")
                    },
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("projects/test1/my-dir/file2")
                    },
                    Stub(Resource){
                        isDirectory()>>true
                        getPath()>>PathUtil.asPath("projects/test1/my-dir/etc")
                    }
            ]
            hasDirectory("projects/test1/not-my-resource") >> false
        }
        expect:
        service.listProjectDirPaths("test1","my-dir")==['my-dir/file1','my-dir/file2','my-dir/etc/']
        service.listProjectDirPaths("test1","not-my-resource")==[]
    }

    void "list project dir paths when tree throws exception"() {
        given:
        service.storage = Stub(StorageTree) {
            hasDirectory("projects/test1/my-dir") >> false
            listDirectory("projects/test1/my-dir") >> {
                throw StorageException.listException(PathUtil.asPath('projects/test1/my-dir'), 'dne')
            }
        }
        expect:
        service.listProjectDirPaths("test1", "my-dir") == []
    }
    void "storage list paths regex"(){
        given:
        service.storage=Stub(StorageTree){
            hasDirectory("projects/test1/my-dir") >> true
            listDirectory("projects/test1/my-dir") >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("projects/test1/my-dir/file1")
                    },
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("projects/test1/my-dir/file2")
                    },
                    Stub(Resource){
                        isDirectory()>>true
                        getPath()>>PathUtil.asPath("projects/test1/my-dir/etc")
                    }
            ]
            hasDirectory("projects/test1/not-my-resource") >> false
        }
        expect:
        service.listProjectDirPaths("test1","my-dir",'file[12]')==['my-dir/file1','my-dir/file2']
        service.listProjectDirPaths("test1","my-dir",'.*/')==['my-dir/etc/']
    }
    void "storage read test"(){
        given:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.storage=Stub(StorageTree){
            getResource("projects/test1/my-resource") >> resStub
        }

        when:
        def baos=new ByteArrayOutputStream()
        def len=service.readProjectFileResource("test1","my-resource",baos)

        then:
        6==len
        'abcdef'==baos.toString()
    }
    void "storage read does not close outputstream"(){
        given:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.storage=Stub(StorageTree){
            getResource("projects/test1/my-resource") >> resStub
        }
        def output=Mock(OutputStream)

        when:
        def len=service.readProjectFileResource("test1","my-resource",output)

        then:
        6==len
        _*output.write(*_)
        0*output.close()
    }
    void "storage read does not exist"(){
        given:

        service.storage=Stub(StorageTree){
            getResource("projects/test1/my-resource") >> {
                throw StorageException.readException(PathUtil.asPath('projects/test1/my-resource'), "does not exist")
            }
        }

        when:
        def baos=new ByteArrayOutputStream()
        def len=service.readProjectFileResource("test1","my-resource",baos)

        then:
        StorageException e = thrown()
        e.message.contains('does not exist')
    }
    void "storage create test"(){
        setup:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.storage=Stub(StorageTree){
            createResource("projects/test1/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }

        when:
        def result=service.createProjectFileResource("test1","my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
    }
    void "storage update test"(){
        setup:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.storage=Stub(StorageTree){
            updateResource("projects/test1/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }

        when:
        def result=service.updateProjectFileResource("test1","my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
    }
    void "storage write test new resource"(){
        given:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/my-resource") >> false
            createResource("projects/test1/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }

        when:
        def result=service.writeProjectFileResource("test1","my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
    }
    void "storage write test existing resource"(){
        given:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/my-resource") >> true
            updateResource("projects/test1/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }

        when:
        def result=service.writeProjectFileResource("test1","my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
    }
    void "storage delete resources recursively"(){
        setup:

        service.storage=Mock(StorageTree){
            1*hasResource({it.path=="projects/test1"}) >> false
            1*hasResource({it.path=="projects/test1/etc"}) >> false
            1*hasDirectory({it.path=="projects/test1"}) >> true
            1*hasDirectory({it.path=="projects/test1/etc"}) >> true
            1*deleteResource({it.path=="projects/test1/file1"}) >> true
            1*deleteResource({it.path=="projects/test1/file2"}) >> true
            1*deleteResource({it.path=="projects/test1/etc/project.properties"}) >> true
            1*listDirectory({it.path=="projects/test1"}) >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("projects/test1/file1")
                    },
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("projects/test1/file2")
                    },
                    Stub(Resource){
                        isDirectory()>>true
                        getPath()>>PathUtil.asPath("projects/test1/etc")
                    }
            ]
            1*listDirectory({it.path=="projects/test1/etc"}) >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("projects/test1/etc/project.properties")
                    }
            ]
        }
        when:

        def result=service.deleteAllProjectFileResources('test1')

        then:
        result

    }
    void "storage delete test existing resource"(){
        given:
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/my-resource") >> true
            deleteResource("projects/test1/my-resource") >> true
        }

        when:
        def result=service.deleteProjectFileResource("test1","my-resource")

        then:
        result
    }
    void "storage delete test missing resource"(){
        given:
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/my-resource") >> false
        }

        when:
        def result=service.deleteProjectFileResource("test1","my-resource")

        then:
        result
    }
    void "storage delete test existing resource fails"(){
        given:
        service.storage=Stub(StorageTree){
            hasResource("projects/test1/my-resource") >> true
            deleteResource("projects/test1/my-resource") >> false
        }

        when:
        def result=service.deleteProjectFileResource("test1","my-resource")

        then:
        !result
    }

    void "create project authorization"(){
        given:

        service.storage=Stub(StorageTree){
            hasDirectory("projects/test1/acls") >> true
            listDirectory("projects/test1/acls") >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("projects/test1/acls/file1.aclpolicy")
                    },
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("projects/test1/acls/file2aclpolicy")
                    },
                    Stub(Resource){
                        isDirectory()>>true
                        getPath()>>PathUtil.asPath("projects/test1/acls/blah")
                    }
            ]
            hasResource("projects/test1/acls/file1.aclpolicy") >> true
            getResource("projects/test1/acls/file1.aclpolicy") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(
                            ('{ description: \'\', \n' +
                                    'by: { username: \'test\' }, \n' +
                                    'for: { resource: [ { equals: { kind: \'zambo\' }, allow: \'x\' } ] } }'
                            ).bytes
                    )
                    getModificationTime() >> new Date()
                }
            }
        }

        when:
        def auth=service.getProjectAuthorization("test1")

        then:
        auth!=null
        auth instanceof RuleEvaluator
        def rules=((RuleEvaluator)auth).getRuleSet().rules
        rules.size()==1
        def rulea=rules.first()
        rulea.allowActions==['x'] as Set
        rulea.description==''
        !rulea.containsMatch
        rulea.equalsMatch
        !rulea.regexMatch
        !rulea.subsetMatch
        rulea.resourceType=='resource'
        rulea.regexResource==null
        rulea.containsResource==null
        rulea.subsetResource==null
        rulea.equalsResource==[kind:'zambo']
        rulea.username=='test'
        rulea.group==null
        rulea.environment!=null
        rulea.environment.key=='project'
        rulea.environment.value=='test1'
        rulea.sourceIdentity=='[project:test1]acls/file1.aclpolicy[1][type:resource][rule: 1]'
    }

    void "mark existing other project as imported"(){
        given:
        def pm1 = Mock(ProjectManager){
            1*existsFrameworkProject('abc')>>true
            1*getFrameworkProject('abc')>>Mock(IRundeckProject){
                1* loadFileResource('etc/project.properties',_) >> {args->
                    args[1].write('test'.bytes)
                    4
                }
                1*storeFileResource('etc/project.properties.imported',{
                    def baos=new ByteArrayOutputStream()
                    Streams.copy(it,baos,true)
                    new String(baos.toByteArray())=='test'
                }) >> 4
                1*deleteFileResource('etc/project.properties') >> true
            }
        }
        when:
        service.markProjectAsImported(pm1, 'abc')

        then:
        true
    }

    void "mark non-existing other project as imported"(){
        given:
        def pm1 = Mock(ProjectManager){
            1*existsFrameworkProject('abc')>>false
        }
        when:
        service.markProjectAsImported(pm1, "abc")

        then:
        true
    }
    void "Test project was imported, dne"(){
        given:
        def pm1 = Mock(ProjectManager){
            1*existsFrameworkProject('abc')>>false
        }
        when:
        def result=service.testProjectWasImported(pm1,'abc')

        then:
        !result
    }
    void "Test project was imported, exists, not imported"(){
        given:
        def pm1 = Mock(ProjectManager){
            1*existsFrameworkProject('abc')>>true
            1*getFrameworkProject('abc') >> Mock(IRundeckProject){
                1* existsFileResource('etc/project.properties.imported') >> false
            }
        }
        when:
        def result=service.testProjectWasImported(pm1,'abc')

        then:
        !result
    }
    void "Test project was imported, exists, was imported"(){
        given:
        def pm1 = Mock(ProjectManager){
            1*existsFrameworkProject('abc')>>true
            1*getFrameworkProject('abc') >> Mock(IRundeckProject){
                1* existsFileResource('etc/project.properties.imported') >> true
            }
        }
        when:
        def result=service.testProjectWasImported(pm1,'abc')

        then:
        result
    }

    void "import project from fs, no projects"(){
        given:
        def pm1 = Mock(ProjectManager){
            1*listFrameworkProjects()>>[]
        }
        when:
        service.importProjectsFromProjectManager(pm1)

        then:
        true
    }
    void "import project from fs, already present"(){
        given:
        def proj1 = new Project(name:'abc').save()
        def pm1 = Mock(ProjectManager){
            1*listFrameworkProjects()>>[
                    Stub(IRundeckProject){
                        getName()>>'abc'
                    }
            ]
            2*existsFrameworkProject('abc')>>true
            2*getFrameworkProject('abc') >> Mock(IRundeckProject){
                //mark as imported
                1* loadFileResource('etc/project.properties',_) >> {args->
                    args[1].write('test'.bytes)
                    4
                }
                1*storeFileResource('etc/project.properties.imported',{
                    def baos=new ByteArrayOutputStream()
                    Streams.copy(it,baos,true)
                    new String(baos.toByteArray())=='test'
                }) >> 4
                1*deleteFileResource('etc/project.properties') >> true
            }
        }
        when:
        service.importProjectsFromProjectManager(pm1)

        then:
        true
    }
    void "import project from fs, already imported"(){
        given:
        def pm1 = Mock(ProjectManager){
            1*listFrameworkProjects()>>[
                    Stub(IRundeckProject){
                        getName()>>'abc'
                    }
            ]
            1*existsFrameworkProject('abc')>>true
            1*getFrameworkProject('abc') >> Mock(IRundeckProject){
                1* existsFileResource('etc/project.properties.imported') >> true

            }
        }
        when:
        service.importProjectsFromProjectManager(pm1)

        then:
        true
    }
    void "import project from fs, not yet imported"(){
        given:
        def projectProps = new Properties()
        projectProps['test']='abc'
        def pm1 = Mock(ProjectManager){
            1*listFrameworkProjects()>>[
                    Stub(IRundeckProject){
                        getName()>>'abc'
                        getProjectProperties()>>projectProps
                    }
            ]
            2*existsFrameworkProject('abc')>>true
            2*getFrameworkProject('abc') >> Mock(IRundeckProject){
                1* existsFileResource('etc/project.properties.imported') >> false

                //mark as imported
                1* loadFileResource('etc/project.properties',_) >> {args->
                    args[1].write('test=abc'.bytes)
                    4
                }
                1*storeFileResource('etc/project.properties.imported',{
                    def props=new Properties()
                    props.load(it)
                    props['test']=='abc'
                }) >> 4
                1*deleteFileResource('etc/project.properties') >> true

            }
        }
        def modDate=new Date(123)
        service.storage=Stub(StorageTree){
            hasResource("projects/abc/etc/project.properties") >> false
            createResource("projects/abc/etc/project.properties",{res->
                def props=new Properties()
                props.load(res.inputStream)
                props['test']=='abc'
            }) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('abc=def'.bytes)
                    getModificationTime() >> modDate
                    getCreationTime() >> modDate
                }
            }
        }
        def properties=new Properties()
        service.frameworkService=Stub(FrameworkService){
            getRundeckFramework() >> Stub(Framework){
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
        }
        service.rundeckNodeService=Mock(NodeService)
        service.projectCache=Mock(LoadingCache)
        when:
        service.importProjectsFromProjectManager(pm1)

        then:
        1*service.rundeckNodeService.refreshProjectNodes('abc')
        0*service.rundeckNodeService.getNodes('abc')
        1*service.projectCache.invalidate('abc')
        Project.findByName('abc')!=null
    }

    def "load project to cache"(){
        setup:
        def description = 'blah'
        def p = new Project(name:'test1', description: description)
        p.save(flush: true)
        def modDate= new Date(123)

        service.storage=Stub(StorageTree){
            hasResource("projects/test1/etc/project.properties") >> true
            getResource("projects/test1/etc/project.properties") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nprojkey=projval\nproject.description='+description).bytes)
                    getModificationTime() >> modDate
                }
            }
        }

        def properties = new Properties()
        properties.setProperty("fwkprop","fwkvalue")

        service.frameworkService=Stub(FrameworkService){
            getRundeckFramework() >> Stub(Framework){
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
        }
        service.rundeckNodeService=Mock(NodeService)
        when:
        def result=service.loadProject('test1')

        then:
        1*service.rundeckNodeService.refreshProjectNodes('test1')
        result!=null
    }

    void "validate project description regex"() {
        setup:

        def props = new Properties()
        props['abc'] = 'def'
        props['project.description'] = 'desc_test'

        service.storage = Stub(StorageTree) {
            hasResource("projects/test1/etc/project.properties") >> false
            createResource("projects/test1/etc/project.properties", { ResourceMeta rm ->
                def tprops = new Properties()
                tprops.load(rm.inputStream)
                rm.meta.size() == 1 && rm.meta[StorageUtil.RES_META_RUNDECK_CONTENT_TYPE] == ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES && tprops['abc'] == 'def'
            }) >> Stub(Resource) {
                getContents() >> Stub(ResourceMeta) {
                    getInputStream() >> new ByteArrayInputStream(('#' + ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES + '\nabc=def').bytes)
                }
            }
        }

        def properties = new Properties()
        properties.setProperty("fwkprop", "fwkvalue")

        service.frameworkService = Stub(FrameworkService) {
            getRundeckFramework() >> Stub(Framework) {
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
        }
        service.rundeckNodeService = Mock(NodeService)
        service.projectCache = Mock(LoadingCache)

        when:

        def result = service.createFrameworkProject('test1', props)

        then:

        result.name == 'test1'

        where:
        description | _
        'description with empty space' | _
        'desc with _ and -'   | _
        'desc with ( )'   | _

    }
}
