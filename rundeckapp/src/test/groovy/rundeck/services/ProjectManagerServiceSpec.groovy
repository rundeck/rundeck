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

import com.dtolabs.rundeck.core.authorization.Authorization
import com.dtolabs.rundeck.core.authorization.LoggingAuthorization
import com.dtolabs.rundeck.core.authorization.RuleEvaluator
import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.IRundeckProject
import com.dtolabs.rundeck.core.common.ProjectManager
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.utils.PropertyLookup
import com.google.common.cache.LoadingCache
import grails.test.hibernate.HibernateSpec
import grails.testing.services.ServiceUnitTest
import org.apache.commons.fileupload.util.Streams
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import org.rundeck.storage.data.DataUtil
import rundeck.Project
import spock.lang.Unroll

class ProjectManagerServiceSpec extends HibernateSpec implements ServiceUnitTest<ProjectManagerService> {

    List<Class> getDomainClasses() { [Project] }

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
        new Project(name:'test1').save()
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
                    getInputStream() >> new ByteArrayInputStream(('#'+ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES+'\nabc=def').bytes)
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
        service.configStorageService=Mock(ConfigStorageService){
            1 * existsFileResource("projects/test1/etc/project.properties") >> true
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
            listDirPaths("projects/test1/my-dir") >> [
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
            listDirPaths("projects/test1/my-dir") >> [
                    "projects/test1/my-dir/file1",
                    "projects/test1/my-dir/file2",
                    "projects/test1/my-dir/etc/"
            ]
            existsDirResource("projects/test1/not-my-resource") >> false
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
        auth instanceof LoggingAuthorization
        auth.authorization instanceof RuleEvaluator
        def rules=((RuleEvaluator)auth.authorization).getRuleSet().rules
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

    void "mark file as imported"() {
        given:
            def other = Mock(IRundeckProject)
        when:
            service.markProjectFileAsImported(other, path)

        then:

            1 * other.loadFileResource(path, _) >> { args ->
                args[1].write('test'.bytes)
                4
            }
            1 * other.storeFileResource(
                path + '.imported', {
                def baos = new ByteArrayOutputStream()
                Streams.copy(it, baos, true)
                new String(baos.toByteArray()) == 'test'
            }
            ) >> 4
            1 * other.deleteFileResource(path) >> true

        where:
            path << ['etc/project.properties', 'readme.md', 'acls/test.aclpolicy']
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
        def pm1 = Stub(ProjectManager){
            listFrameworkProjects()>>[
                    Mock(IRundeckProject){
                        _ * getName() >> 'abc'
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
            ]
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
                Mock(IRundeckProject){
                    _*getName()>>'abc'
                    1* existsFileResource('etc/project.properties.imported') >> true
                }
            ]
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
                Mock(IRundeckProject){
                    _*getName()>>'abc'
                    _*getProjectProperties()>>projectProps
                    1* existsFileResource('etc/project.properties.imported') >> false

                    1 * listDirPaths('')>>['/etc/']
                    1 * listDirPaths('/etc/')>>['/etc/project.properties']
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
            ]
        }
        def modDate=new Date(123)
        service.configStorageService=Stub(ConfigStorageService){
            existsFileResource("projects/abc/etc/project.properties") >> false
            createFileResource("projects/abc/etc/project.properties",{ins->
                def props=new Properties()
                props.load(ins)
                props['test']=='abc'
            },[(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE):ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES]) >> Stub(Resource){
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
        service.projectCache=Mock(LoadingCache){
            1 * get('abc')>>{
                 service.loadProject('abc')
            }
        }
        when:
        service.importProjectsFromProjectManager(pm1)

        then:
        2*service.rundeckNodeService.refreshProjectNodes('abc')
        0*service.rundeckNodeService.getNodes('abc')
        1*service.projectCache.invalidate('abc')
        Project.findByName('abc')!=null
    }
    @Unroll
    void "import project from fs, not yet imported with readme"(){
        given:
        def projectProps = new Properties()
        projectProps['test']='abc'
        service.configurationService=Mock(ConfigurationService){
            1 * getString('projectsStorageImportResources')>>{
                configSet?'always':null
            }
        }
        if(projExists){
            def proj1 = new Project(name:'abc').save()
        }
        def pm1 = Stub(ProjectManager){
            listFrameworkProjects()>>[
                Mock(IRundeckProject){
                    getName()>>'abc'
                    getProjectProperties()>>projectProps
                    1 * listDirPaths('')>>['/motd.md','/readme.md','/etc/']
                    1 * listDirPaths('/etc/')>>['/etc/project.properties']
                    1* loadFileResource('/motd.md',_)>>{
                        it[1].write('motddata'.bytes)
                        1
                    }
                    1* loadFileResource('/readme.md',_)>>{
                        it[1].write('readmedata'.bytes)
                        1
                    }

                    1* existsFileResource('etc/project.properties.imported') >> false

                    //mark as imported
                    1* loadFileResource('etc/project.properties', _) >> { args->
                        args[1].write('test=abc'.bytes)
                        4
                    }
                    1* storeFileResource('etc/project.properties.imported', {
                        def props=new Properties()
                        props.load(it)
                        props['test']=='abc'
                    }) >> 4

                    1* deleteFileResource('etc/project.properties') >> true
                    1*deleteFileResource('/motd.md') >> true
                    1*deleteFileResource('/readme.md') >> true
                    0*deleteFileResource(_)
                }
            ]
        }
        def modDate=new Date(123)
        service.configStorageService=Mock(ConfigStorageService){
            (projExists ? 1 : 2) * existsFileResource("projects/abc/etc/project.properties") >> projExists
            (projExists ? 0 : 1) * createFileResource("projects/abc/etc/project.properties", { res->
                def props=new Properties()
                props.load(res)
                props['test']=='abc'
            },[(StorageUtil.RES_META_RUNDECK_CONTENT_TYPE):ProjectManagerService.MIME_TYPE_PROJECT_PROPERTIES]) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('abc=def'.bytes)
                    getModificationTime() >> modDate
                    getCreationTime() >> modDate
                }
            }
            1 * createFileResource("projects/abc/motd.md",{res->
                res.text=='motddata'
            },[:]) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('asdf'.bytes)
                    getModificationTime() >> modDate
                    getCreationTime() >> modDate
                }
            }
            1 * createFileResource("projects/abc/readme.md",{res->
                res.text=='readmedata'
            },[:]) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('asdf'.bytes)
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
        service.projectCache=Mock(LoadingCache){
            1 * get('abc')>>{
                service.loadProject('abc')
            }
        }
        when:
        service.importProjectsFromProjectManager(pm1)

        then:
        _*service.rundeckNodeService.refreshProjectNodes('abc')
        0*service.rundeckNodeService.getNodes('abc')
        _*service.projectCache.invalidate('abc')
        Project.findByName('abc')!=null
        where:
            projExists | configSet
            false       | false
            true        | true
    }
    void "import project from fs, not yet imported with acls"(){
        given:
        def projectProps = new Properties()
        projectProps['test']='abc'
        def pm1 = Stub(ProjectManager){
            listFrameworkProjects()>>[
                Mock(IRundeckProject){
                    getName()>>'abc'
                    getProjectProperties()>>projectProps
                    1 * listDirPaths('')>>['/motd.md','/readme.md','/etc/','/acls/']
                    1 * listDirPaths('/etc/')>>['/etc/project.properties']
                    1 * listDirPaths('/acls/')>>['/acls/test1.aclpolicy']
                    1* loadFileResource('/motd.md',_)>>{
                        it[1].write('motddata'.bytes)
                        1
                    }
                    1* loadFileResource('/readme.md',_)>>{
                        it[1].write('readmedata'.bytes)
                        1
                    }
                    1* loadFileResource('/acls/test1.aclpolicy',_)>>{
                        it[1].write('acldata'.bytes)
                        1
                    }
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
                    1*deleteFileResource('/motd.md') >> true
                    1*deleteFileResource('/readme.md') >> true
                    1*deleteFileResource('/acls/test1.aclpolicy') >> true
                    0*deleteFileResource(_)

                }
            ]
        }
        def modDate=new Date(123)
        service.configStorageService=Mock(ConfigStorageService){
            2 * existsFileResource("projects/abc/etc/project.properties") >> false
            1 * createFileResource("projects/abc/etc/project.properties",{res->
                def props=new Properties()
                props.load(res)
                props['test']=='abc'
            }) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('abc=def'.bytes)
                    getModificationTime() >> modDate
                    getCreationTime() >> modDate
                }
            }
            1 * createFileResource("projects/abc/motd.md",{res->
                res.text=='motddata'
            }) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('asdf'.bytes)
                    getModificationTime() >> modDate
                    getCreationTime() >> modDate
                }
            }
            1 * createFileResource("projects/abc/readme.md",{res->
                res.text=='readmedata'
            }) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('asdf'.bytes)
                    getModificationTime() >> modDate
                    getCreationTime() >> modDate
                }
            }
            1 * createFileResource("projects/abc/acls/test1.aclpolicy",{res->
                res.text=='acldata'
            }) >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('asdf'.bytes)
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
        service.projectCache=Mock(LoadingCache){
            1 * get('abc')>>{
                service.loadProject('abc')
            }
        }
        when:
        service.importProjectsFromProjectManager(pm1)

        then:
        2*service.rundeckNodeService.refreshProjectNodes('abc')
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
        props['project.description'] = 'desc_test'

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
}
