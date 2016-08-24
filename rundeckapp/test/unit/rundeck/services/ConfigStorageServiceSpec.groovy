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

import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.core.storage.StorageTree
import grails.test.mixin.TestFor
import org.rundeck.storage.api.PathUtil
import org.rundeck.storage.api.Resource
import org.rundeck.storage.api.StorageException
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicInteger

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(ConfigStorageService)
class ConfigStorageServiceSpec extends Specification {

    def setup() {
    }

    def cleanup() {
    }


    void "storage exists test"(){
        given:
        service.rundeckConfigStorageTree=Stub(StorageTree){
            hasResource("/my-resource") >> true
            hasResource("/not-my-resource") >> false
        }
        expect:
        service.existsFileResource("my-resource")
        !service.existsFileResource("not-my-resource")
    }
    void "storage dir exists test"(){
        given:
        service.rundeckConfigStorageTree=Stub(StorageTree){
            hasDirectory("/my-resource") >> true
            hasDirectory("/not-my-resource") >> false
        }
        expect:
        service.existsDirResource("my-resource")
        !service.existsDirResource("not-my-resource")
    }
    void "storage list paths"(){
        given:
        service.rundeckConfigStorageTree=Stub(StorageTree){
            hasDirectory("/my-dir") >> true
            listDirectory("/my-dir") >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("my-dir/file1")
                    },
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("my-dir/file2")
                    },
                    Stub(Resource){
                        isDirectory()>>true
                        getPath()>>PathUtil.asPath("my-dir/etc")
                    }
            ]
            hasDirectory("/not-my-resource") >> false
        }
        expect:
        service.listDirPaths("my-dir")==['my-dir/file1','my-dir/file2','my-dir/etc/']
        service.listDirPaths("not-my-resource")==[]
    }
    void "storage list paths regex"(){
        given:
        service.rundeckConfigStorageTree=Stub(StorageTree){
            hasDirectory("/my-dir") >> true
            listDirectory("/my-dir") >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("my-dir/file1")
                    },
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("my-dir/file2")
                    },
                    Stub(Resource){
                        isDirectory()>>true
                        getPath()>>PathUtil.asPath("my-dir/etc")
                    }
            ]
            hasDirectory("/not-my-resource") >> false
        }
        expect:
        service.listDirPaths("my-dir",'file[12]')==['my-dir/file1','my-dir/file2']
        service.listDirPaths("my-dir",'.*/')==['my-dir/etc/']
    }
    void "storage read test"(){
        given:
        service.rundeckConfigStorageTree=Stub(StorageTree){
            getResource("/my-resource") >> Stub(Resource){
                getContents()>> Stub(ResourceMeta){
                    getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
                }
            }
        }

        when:
        def baos=new ByteArrayOutputStream()
        def len=service.loadFileResource("my-resource",baos)

        then:
        len==6L
        'abcdef'==baos.toString()
    }
    void "storage read does not exist"(){
        given:

        service.rundeckConfigStorageTree=Stub(StorageTree){
            getResource("/my-resource") >> {
                throw StorageException.readException(PathUtil.asPath('my-resource'), "does not exist")
            }
        }

        when:
        def baos=new ByteArrayOutputStream()
        def len=service.loadFileResource("my-resource",baos)

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
        service.rundeckConfigStorageTree=Stub(StorageTree){
            createResource("/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }

        when:
        def result=service.createFileResource("my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
    }
    void "storage create listeners test"(){
        setup:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.rundeckConfigStorageTree=Stub(StorageTree){
            createResource("/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }
        def AtomicInteger createdCount = new AtomicInteger(0)
        def AtomicInteger modifiedCount = new AtomicInteger(0)
        def AtomicInteger deletedCount = new AtomicInteger(0)

        def listener1=[resourceCreated:{p->createdCount.incrementAndGet()},
                       resourceModified:{p->modifiedCount.incrementAndGet()},
                       resourceDeleted:{p->deletedCount.incrementAndGet()}] as StorageManagerListener
        def listener2=[resourceCreated:{p->createdCount.incrementAndGet()},
                       resourceModified:{p->modifiedCount.incrementAndGet()},
                       resourceDeleted:{p->deletedCount.incrementAndGet()}] as StorageManagerListener

        service.addListener(listener1)
        service.addListener(listener2)
        when:
        def result=service.createFileResource("my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        createdCount.get()==2
        modifiedCount.get()==0
        deletedCount.get()==0
    }
    void "storage update test"(){
        setup:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.rundeckConfigStorageTree=Stub(StorageTree){
            updateResource("/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }

        when:
        def result=service.updateFileResource("my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
    }
    void "storage update listeners test"(){
        setup:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.rundeckConfigStorageTree=Stub(StorageTree){
            updateResource("/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }
        def AtomicInteger createdCount = new AtomicInteger(0)
        def AtomicInteger modifiedCount = new AtomicInteger(0)
        def AtomicInteger deletedCount = new AtomicInteger(0)

        def listener1=[resourceCreated:{p->createdCount.incrementAndGet()},
                       resourceModified:{p->modifiedCount.incrementAndGet()},
                       resourceDeleted:{p->deletedCount.incrementAndGet()}] as StorageManagerListener
        def listener2=[resourceCreated:{p->createdCount.incrementAndGet()},
                       resourceModified:{p->modifiedCount.incrementAndGet()},
                       resourceDeleted:{p->deletedCount.incrementAndGet()}] as StorageManagerListener

        service.addListener(listener1)
        service.addListener(listener2)
        when:
        def result=service.updateFileResource("my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
        createdCount.get()==0
        modifiedCount.get()==2
        deletedCount.get()==0
    }
    void "storage write test new resource"(){
        given:
        def meta=Stub(ResourceMeta){
            getInputStream() >> new ByteArrayInputStream('abcdef'.bytes)
        }
        def resStub = Stub(Resource){
            getContents()>> meta
        }
        service.rundeckConfigStorageTree=Stub(StorageTree){
            hasResource("/my-resource") >> false
            createResource("/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }

        when:
        def result=service.writeFileResource("my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

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
        service.rundeckConfigStorageTree=Stub(StorageTree){
            hasResource("/my-resource") >> true
            updateResource("/my-resource",{ResourceMeta rm->
                rm.meta['a']=='b' && null!=rm.inputStream
            }) >> resStub
        }

        when:
        def result=service.writeFileResource("my-resource",new ByteArrayInputStream('abcdef'.bytes),[a:'b'])

        then:
        resStub==result
    }
    void "storage delete resources recursively"(){
        setup:

        service.rundeckConfigStorageTree=Mock(StorageTree){
            1*hasResource({it.path=="projects/test1"}) >> false
            1*hasResource({it.path=="etc"}) >> false
            1*hasDirectory({it.path=="projects/test1"}) >> true
            1*hasDirectory({it.path=="etc"}) >> true
            1*deleteResource({it.path=="file1"}) >> true
            1*deleteResource({it.path=="file2"}) >> true
            1*deleteResource({it.path=="etc/project.properties"}) >> true
            1*listDirectory({it.path=="projects/test1"}) >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("file1")
                    },
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("file2")
                    },
                    Stub(Resource){
                        isDirectory()>>true
                        getPath()>>PathUtil.asPath("etc")
                    }
            ]
            1*listDirectory({it.path=="etc"}) >> [
                    Stub(Resource){
                        isDirectory()>>false
                        getPath()>>PathUtil.asPath("etc/project.properties")
                    }
            ]
        }
        when:

        def result=service.deleteAllFileResources('projects/test1')

        then:
        result

    }
    void "storage delete test existing resource"(){
        given:
        service.rundeckConfigStorageTree=Stub(StorageTree){
            hasResource("/my-resource") >> true
            deleteResource("/my-resource") >> true
        }

        when:
        def result=service.deleteFileResource("my-resource")

        then:
        result
    }
    void "storage delete listener test"(){
        given:
        service.rundeckConfigStorageTree=Stub(StorageTree){
            hasResource("/my-resource") >> true
            deleteResource("/my-resource") >> true
        }
        def AtomicInteger createdCount = new AtomicInteger(0)
        def AtomicInteger modifiedCount = new AtomicInteger(0)
        def AtomicInteger deletedCount = new AtomicInteger(0)

        def listener1=[resourceCreated:{p->createdCount.incrementAndGet()},
                       resourceModified:{p->modifiedCount.incrementAndGet()},
                       resourceDeleted:{p->deletedCount.incrementAndGet()}] as StorageManagerListener
        def listener2=[resourceCreated:{p->createdCount.incrementAndGet()},
                       resourceModified:{p->modifiedCount.incrementAndGet()},
                       resourceDeleted:{p->deletedCount.incrementAndGet()}] as StorageManagerListener

        service.addListener(listener1)
        service.addListener(listener2)
        when:
        def result=service.deleteFileResource("my-resource")

        then:
        result
        createdCount.get()==0
        modifiedCount.get()==0
        deletedCount.get()==2
    }
    void "storage delete test missing resource"(){
        given:
        service.rundeckConfigStorageTree=Stub(StorageTree){
            hasResource("/my-resource") >> false
        }

        when:
        def result=service.deleteFileResource("my-resource")

        then:
        result
    }
    void "storage delete test existing resource fails"(){
        given:
        service.rundeckConfigStorageTree=Stub(StorageTree){
            hasResource("/my-resource") >> true
            deleteResource("/my-resource") >> false
        }

        when:
        def result=service.deleteFileResource("my-resource")

        then:
        !result
    }

}
