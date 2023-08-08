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

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.utils.PropertyLookup
import com.dtolabs.rundeck.server.projects.RundeckProject
import com.dtolabs.rundeck.server.projects.RundeckProjectConfig
import com.google.common.cache.LoadingCache
import grails.events.bus.EventBus
import grails.testing.gorm.DataTest
import grails.testing.services.ServiceUnitTest
import org.rundeck.app.data.model.v1.project.RdProject
import org.rundeck.app.data.providers.GormProjectDataProvider
import org.rundeck.app.data.providers.GormTokenDataProvider
import org.rundeck.app.data.providers.v1.project.RundeckProjectDataProvider
import org.rundeck.app.grails.events.AppEvents
import org.rundeck.spi.data.DataAccessException

import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import org.rundeck.storage.data.DataUtil
import rundeck.Project
import rundeck.User
import rundeck.services.data.AuthTokenDataService
import rundeck.services.data.ProjectDataService
import spock.lang.Specification
import spock.lang.Unroll

class ProjectManagerServiceSpec extends Specification implements ServiceUnitTest<ProjectManagerService>, DataTest {

    def setupSpec() { mockDomains Project }

    def setup() {


        mockDataService(ProjectDataService)
        GormProjectDataProvider provider = new GormProjectDataProvider()
        provider.projectDataService = applicationContext.getBean(ProjectDataService)
        service.projectDataProvider = provider
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
        service.configStorageService=Stub(ConfigStorageService)

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

        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/etc/project.properties") >> true
            getFileResource("projects/test1/etc/project.properties") >> Stub(Resource){
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

        service.configStorageService=Mock(ConfigStorageService){
            1*existsFileResource("projects/test1/etc/project.properties") >> true
            1*existsFileResource("projects/test1/readme.md") >> true
            1*existsFileResource("projects/test1/motd.md") >> true
            getFileResource("projects/test1/etc/project.properties") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nprojkey=projval\nproject.description='+description).bytes)
                    getModificationTime() >> modDate
                }
            }
            1*getFileResource("projects/test1/readme.md") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('blah readme'.bytes)
                    getModificationTime() >> modDate
                }
            }
            1*getFileResource("projects/test1/motd.md") >> Stub(Resource){
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

        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/etc/project.properties") >> true
            getFileResource("projects/test1/etc/project.properties") >> Stub(Resource){
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

        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/etc/project.properties") >> false
            createFileResource("projects/test1/etc/project.properties",
                               { InputStream is ->
                                   def tprops = new Properties()
                                   tprops.load(is)
                                   tprops['abc'] == 'def'
                               },[(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE):ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES]
            ) >> Stub(Resource){
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
        service.targetEventBus=Mock(EventBus)
        service.configurationService = Mock(ConfigurationService){
            getString("project.defaults.nodeExecutor", "sshj-ssh") >> "sshj-ssh"
            getString("project.defaults.fileCopier", "sshj-scp") >> "sshj-scp"
        }
        when:

        def result = service.createFrameworkProject('test1',props)

        then:
        1*service.projectCache.invalidate('test1')
        1*service.rundeckNodeService.refreshProjectNodes('test1')
        0*service.rundeckNodeService.getNodes('test1')
        1*service.eventBus.notify(AppEvents.PROJECT_CONFIG_CHANGED, {
            it.project=='test1'
            it.props.abc=='def'
            it.props.'project.name'=='test1'
            it.changedKeys.containsAll(it.props.keySet())
        })

        result.name=='test1'
        (2+ProjectManagerService.DEFAULT_PROJ_PROPS.size())==result.getProjectProperties().size()
        'test1'==result.getProjectProperties().get('project.name')
        'def'==result.getProjectProperties().get('abc')

        null!=Project.findByName('test1')
    }

    void "create project strict already exists"(){
        setup:
        Project p = new Project(name:'test1').save(flush: true)
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

        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/etc/project.properties") >> false
            createFileResource("projects/test1/etc/project.properties",
                               { InputStream is ->
                                   def tprops = new Properties()
                                   tprops.load(is)
                                   tprops['abc'] == 'def'
                               },[(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE):ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES]
            ) >> Stub(Resource){
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
        service.configurationService = Mock(ConfigurationService){
            getString("project.defaults.nodeExecutor", "sshj-ssh") >> "sshj-ssh"
            getString("project.defaults.fileCopier", "sshj-scp") >> "sshj-scp"
        }
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
        DataAccessException e = thrown()
        e.message.contains('does not exist')
    }

    void "remove project does exist"(){
        setup:
        def p = new Project(name:'test1')
        p.save(flush: true)

        service.configStorageService=Mock(ConfigStorageService){
            1*deleteAllFileResources("projects/test1")
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
        props1['abc']=abcval
        new Project(name:'test1').save(flush: true)
        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/etc/project.properties") >> true
            getFileResource("projects/test1/etc/project.properties") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nabc=def').bytes)
                }
            }
            updateFileResource("projects/test1/etc/project.properties",{rmi->
                def tprops=new Properties()
                tprops.load(rmi)
                tprops['abc']=='def'
                tprops['def']=='ghi'
            },[(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE):ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES]) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nabc='+abcval+'\ndef=ghi').bytes)
                }
            }
        }

        service.rundeckNodeService=Mock(NodeService)
        service.projectCache=Mock(LoadingCache)
        service.targetEventBus=Mock(EventBus)
        when:
        def res=service.mergeProjectProperties('test1',props1,[] as Set)

        then:
        1*service.projectCache.invalidate('test1')
        1*service.rundeckNodeService.refreshProjectNodes('test1')
        1*service.eventBus.notify(AppEvents.PROJECT_CONFIG_CHANGED, {
            it.project=='test1'
            it.props==[abc:abcval,def:'ghi','project.name':'test1']
            it.changedKeys.size()==changekeys.size()
            it.changedKeys.containsAll(changekeys)
        })

        res != null
        res.config.size() == 3
        abcval == res.config['abc']
        'ghi' == res.config['def']
        'test1' == res.config['project.name']
        where:
            abcval | changekeys
            'def' | ['def','project.name']
            'zzz' | ['abc','def','project.name']
    }

    @Unroll
    void "merge project properties without existing resource"() {
        setup:
        Properties props1 = new Properties()
        props1['def'] = 'ghi'
        new Project(name: 'test1').save(flush: true)
        service.configStorageService=Stub(ConfigStorageService) {
            existsFileResource("projects/test1/etc/project.properties") >> false
            updateFileResource(
                "projects/test1/etc/project.properties", { ins ->
                def tprops = new Properties()
                tprops.load(ins)
                tprops['def'] == 'ghi'},
                [(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE):ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES]
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
        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/etc/project.properties") >> true
            getFileResource("projects/test1/etc/project.properties") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nabc=def\nproject.name=test1').bytes)
                }
            }
            updateFileResource("projects/test1/etc/project.properties",{InputStream inputStream->
                def tprops=new Properties()
                tprops.load(inputStream)
                tprops['abc']==null
                tprops['def']=='ghi'
            },
                               [(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE):ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES]) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\ndef=ghi').bytes)
                }
            }
        }

        service.rundeckNodeService=Mock(NodeService)
        service.projectCache=Mock(LoadingCache)
        service.targetEventBus=Mock(EventBus)

        when:
        def res=service.setProjectProperties('test1',props1)

        then:

        1*service.projectCache.invalidate('test1')
        1*service.rundeckNodeService.refreshProjectNodes('test1')
        1*service.eventBus.notify(AppEvents.PROJECT_CONFIG_CHANGED, {
            it.project=='test1'
            it.props==[def:'ghi','project.name':'test1']
            it.changedKeys.size()==2
            it.changedKeys.containsAll(['abc', 'def'])
        })
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
        service.configStorageService=Mock(ConfigStorageService){
            _ * existsFileResource("projects/test1/etc/project.properties") >> true
            1 * getFileResource("projects/test1/etc/project.properties") >> Stub(Resource){
                getContents() >> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\na=b').bytes)
                }
            }
            1 * updateFileResource("projects/test1/etc/project.properties",{ins->
                def tprops=new Properties()
                tprops.load(ins)
                tprops['abc']==null
                tprops['def']=='ghi'
                tprops['project.name']=='test1'
            },[(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE):ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES]) >> Mock(Resource){
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

    void "merge project properties"(){
        given:
        def projectData = Mock(RdProject) {
            getName() >> "project"
        }
        RundeckProject rundeckProject = new RundeckProject(projectData, null, service)
        RundeckProjectDataProvider providerMock = Mock(RundeckProjectDataProvider){
            findByName("project") >> Mock(RdProject)
        }
        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/my-resource") >> true
            existsFileResource("projects/test1/not-my-resource") >> false
        }

        service.projectDataProvider = providerMock

        def properties = new Properties()
        properties.setProperty("fwkprop","fwkvalue")
        properties.setProperty("project.description", "desc")

        service.frameworkService=Stub(FrameworkService){
            getRundeckFramework() >> Stub(Framework){
                getPropertyLookup() >> PropertyLookup.create(properties)
            }
        }
        service.rundeckNodeService=Mock(NodeService)

        when:
        service.mergeProjectProperties(rundeckProject, properties, null)

        then:
        rundeckProject.projectConfig.name == "project"
        rundeckProject.nodesFactory == service.rundeckNodeService
    }

    void "storage exists test"(){
        given:
        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/my-resource") >> true
            existsFileResource("projects/test1/not-my-resource") >> false
        }
        expect:
        service.existsProjectFileResource("test1","my-resource")
        !service.existsProjectFileResource("test1","not-my-resource")
    }
    void "storage dir exists test"(){
        given:
        service.configStorageService=Stub(ConfigStorageService){
            existsDirResource("projects/test1/my-resource") >> true
            existsDirResource("projects/test1/not-my-resource") >> false
        }
        expect:
        service.existsProjectDirResource("test1","my-resource")
        !service.existsProjectDirResource("test1","not-my-resource")
    }
    void "storage list paths"(){
        given:
        service.configStorageService=Stub(ConfigStorageService){
            existsDirResource("projects/test1/my-dir") >> true
            listDirPaths("projects/test1/my-dir", null) >> [
                  "projects/test1/my-dir/file1",
                  "projects/test1/my-dir/file2",
                  "projects/test1/my-dir/etc/"
            ]
            existsDirResource("projects/test1/not-my-resource") >> false
        }
        expect:
        service.listProjectDirPaths("test1","my-dir")==['my-dir/file1','my-dir/file2','my-dir/etc/']
        service.listProjectDirPaths("test1","not-my-resource")==[]
    }
    def "rewrite prefix"(){
        expect:
            ProjectManagerService.rewritePrefix('proj','projects/proj/apath/bob')=='apath/bob'
            ProjectManagerService.rewritePrefix('proj','projects/proj/apath/bob/')=='apath/bob/'
    }

    void "list project dir paths when tree throws exception"() {
        given:
        service.configStorageService=Stub(ConfigStorageService) {
            existsDirResource("projects/test1/my-dir") >> false
            listDirPaths("projects/test1/my-dir") >> {
                throw StorageException.listException(PathUtil.asPath('projects/test1/my-dir'), 'dne')
            }
        }
        expect:
        service.listProjectDirPaths("test1", "my-dir") == []
    }
    void "storage list paths regex"(){
        given:
        service.configStorageService=Stub(ConfigStorageService){
            existsDirResource("projects/test1/my-dir") >> true
            listDirPaths("projects/test1/my-dir", pat) >> resultDirPaths
            existsDirResource("projects/test1/not-my-resource") >> false
        }
        expect:
        service.listProjectDirPaths("test1", 'my-dir', pat)==expect
        where:
            pat        | resultDirPaths                                                  | expect
            'file[12]' | ["projects/test1/my-dir/file1", "projects/test1/my-dir/file2",] | ['my-dir/file1', 'my-dir/file2']
            '.*/'      | ["projects/test1/my-dir/etc/"]                                  | ['my-dir/etc/']
    }
    void "storage read test"(){
        given:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.configStorageService=Stub(ConfigStorageService){
            getFileResource("projects/test1/my-resource") >> resStub
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
        service.configStorageService=Stub(ConfigStorageService){
            getFileResource("projects/test1/my-resource") >> resStub
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

        service.configStorageService=Stub(ConfigStorageService){
            getFileResource("projects/test1/my-resource") >> {
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
        service.configStorageService=Stub(ConfigStorageService){
            createFileResource("projects/test1/my-resource",!null,[a:'b']) >> resStub
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
        service.configStorageService=Stub(ConfigStorageService){
            updateFileResource("projects/test1/my-resource",!null,[a:'b']) >> resStub
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
        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/my-resource") >> false
            createFileResource("projects/test1/my-resource",!null,[a:'b']) >> resStub
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
        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/my-resource") >> true
            updateFileResource("projects/test1/my-resource",!null,[a:'b']) >> resStub
        }

        when:
        def result=service.writeProjectFileResource("test1","my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
    }
    void "storage delete resources recursively"(){
        setup:

        service.configStorageService=Mock(ConfigStorageService){
            1 * deleteAllFileResources('projects/test1')>>true
        }
        when:

        def result=service.deleteAllProjectFileResources('test1')

        then:
        result

    }
    void "storage delete test existing resource"(){
        given:
        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/my-resource") >> true
            deleteFileResource("projects/test1/my-resource") >> true
        }

        when:
        def result=service.deleteProjectFileResource("test1","my-resource")

        then:
        result
    }
    void "storage delete test missing resource"(){
        given:
        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/my-resource") >> false
        }

        when:
        def result=service.deleteProjectFileResource("test1","my-resource")

        then:
        result
    }
    void "storage delete test existing resource fails"(){
        given:
        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/my-resource") >> true
            deleteFileResource("projects/test1/my-resource") >> false
        }

        when:
        def result=service.deleteProjectFileResource("test1","my-resource")

        then:
        !result
    }




    def "load project to cache"(){
        setup:
        def description = 'blah'
        def p = new Project(name:'test1', description: description)
        p.save(flush: true)
        def modDate= new Date(123)

        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/test1/etc/project.properties") >> true
            getFileResource("projects/test1/etc/project.properties") >> Stub(Resource){
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

    def "Count projects empty"() {
        expect:
            0 == service.countFrameworkProjects()
    }

    def "Count projects"() {
        setup:
            def p = new Project(name: 'test1', description: '')
            p.save(flush: true)
            def p2 = new Project(name: 'test2', description: '')
            p2.save(flush: true)
        expect:
            2 == service.countFrameworkProjects()
    }

    void "validate project description regex"() {
        setup:

        def props = new Properties()
        props['abc'] = 'def'
        props['project.description'] = description

        service.configStorageService=Stub(ConfigStorageService) {
            existsFileResource("projects/test1/etc/project.properties") >> false
            createFileResource("projects/test1/etc/project.properties", { resi ->
                def tprops = new Properties()
                tprops.load(resi)
                tprops['abc'] == 'def'
            },[(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE):ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES]) >> Stub(Resource) {
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
        service.configurationService = Mock(ConfigurationService){
            getString("project.defaults.nodeExecutor", "sshj-ssh") >> "sshj-ssh"
            getString("project.defaults.fileCopier", "sshj-scp") >> "sshj-scp"
        }
        service.projectDataProvider=Mock(RundeckProjectDataProvider){
            1 * findByName('test1')
            1 * create({
                it.description==description
                it.name=='test1'
            })
        }

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

    def "nonAuthorizingProjectStorageTreeSubpath should record timestamps create"() {
        given:
            def input = new ByteArrayInputStream('test'.bytes)
            def baseTree = Mock(StorageTree)
            service.configStorageService = Mock(ConfigStorageService) {
                storageTreeSubpath(_) >> baseTree
            }
            def tree = service.nonAuthorizingProjectStorageTreeSubpath('test', 'a/path')
        when:
            def result = tree.createResource('test', DataUtil.withStream(input, [:], StorageUtil.factory()))
        then:
            1 * baseTree.createResource(_, { it.modificationTime && it.creationTime })>>Mock(Resource)
    }

    def "nonAuthorizingProjectStorageTreeSubpath should record timestamps update"() {
        given:
            def input = new ByteArrayInputStream('test'.bytes)
            def baseTree = Mock(StorageTree)
            service.configStorageService = Mock(ConfigStorageService) {
                storageTreeSubpath(_) >> baseTree
            }
            def tree = service.nonAuthorizingProjectStorageTreeSubpath('test', 'a/path')
        when:
            def result2 = tree.updateResource('test', DataUtil.withStream(input, [:], StorageUtil.factory()))
        then:
            1 * baseTree.updateResource(_, { it.modificationTime && !it.creationTime })>>Mock(Resource)
            0 * baseTree._(*_)
    }

    def "getProjectDescription"() {
        given:
            def p = new Project(name: 'test1')
            p.description=desc
            p.save(flush: true)
        when:
            def result = service.getProjectDescription('test1')
        then:
            result == expected
        where:
            desc            | expected
            'a description' | 'a description'
            null            | null
    }

    def "getKeyDiff"() {
        given:
            Properties orig = new Properties()
            orig.putAll([a: 'aaa', b: 'bbb'])
            Properties newvals = new Properties()
            newvals.putAll(newprops)
        when:
            def result = ProjectManagerService.getKeyDiff(orig, newvals)
        then:
            result == expected.toSet()
        where:
            newprops             | expected
            [a: 'aaa', b: 'bbb'] | []
            [a: 'XXX', b: 'bbb'] | ['a']
            [b: 'bbb']           | ['a']
            [a: 'aaa', b: 'XXX'] | ['b']
            [a: 'aaa']           | ['b']
            [:]                  | ['a', 'b']
    }
}
