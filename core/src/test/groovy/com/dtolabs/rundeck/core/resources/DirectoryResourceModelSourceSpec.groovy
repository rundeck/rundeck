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

package com.dtolabs.rundeck.core.resources

import com.dtolabs.rundeck.core.common.Framework
import com.dtolabs.rundeck.core.common.FrameworkProject
import com.dtolabs.rundeck.core.common.INodeSet
import com.dtolabs.rundeck.core.common.NodeEntryImpl
import com.dtolabs.rundeck.core.common.NodeSetImpl
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParser
import com.dtolabs.rundeck.core.resources.format.ResourceFormatParserService
import com.dtolabs.rundeck.core.tools.AbstractBaseTest
import com.dtolabs.rundeck.core.utils.FileUtils
import com.dtolabs.utils.Streams
import spock.lang.Specification

/**
 * Created by greg on 4/27/16.
 */
class DirectoryResourceModelSourceSpec extends Specification {
    public static final String PROJECT_NAME = 'DirectoryResourceModelSourceSpec'
    Framework framework
    FrameworkProject testProject
    File directory

    def setup() {
        framework = AbstractBaseTest.createTestFramework()
        testProject = framework.getFrameworkProjectMgr().createFrameworkProject(PROJECT_NAME)
        directory = new File(testProject.getBaseDir(), "testGetNodesMultiFile");
        FileUtils.deleteDir(directory)
        directory.mkdirs();
    }

    def cleanup() {
        if(directory.exists()){
            FileUtils.deleteDir(directory)
        }
        framework.getFrameworkProjectMgr().removeFrameworkProject(PROJECT_NAME)
    }


    def "test file npe"(){
        given:
        def file = new File(directory, "test.nodes")
        file<<'nodes'
        framework.resourceFormatParserService.registerInstance('testnodes',Mock(ResourceFormatParser) {
            getFileExtensions() >> new HashSet<String>(['nodes'])
            parseDocument(_)>>{
                throw new NullPointerException("test npe")
            }
        })
        def path = file.absolutePath

        Properties props = ["project": PROJECT_NAME, "directory": directory.getAbsolutePath()]

        DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(props);
        final DirectoryResourceModelSource directoryNodesProvider = new DirectoryResourceModelSource(framework);
        directoryNodesProvider.configure(config);

        when:

        final INodeSet nodes = directoryNodesProvider.getNodes();

        then:
        nodes.nodes.size()==0
        directoryNodesProvider.modelSourceErrors != null
        directoryNodesProvider.modelSourceErrors == ["Error loading file: "+path+": java.lang.NullPointerException: test npe"]
    }
    def "multiple files with one causing npe"(){
        given:
        def file = new File(directory, "test.nodes")
        def path = file.absolutePath
        def file2 = new File(directory, "test2.nodes")
        file<<'nodes'
        file2 << 'nodes2'
        def nodes2 = new NodeSetImpl()
        nodes2.putNode(new NodeEntryImpl("anode1"))

        framework.resourceFormatParserService.registerInstance('testnodes',Mock(ResourceFormatParser) {
            getFileExtensions() >> new HashSet<String>(['nodes'])
            parseDocument(_ as InputStream) >> { args ->
                ByteArrayOutputStream baos = new ByteArrayOutputStream()

                Streams.copyStream(args[0], baos)
                def string = new String(baos.toByteArray())
                if (string == 'nodes') {
                    throw new NullPointerException("test npe")
                } else {
                    return nodes2
                }
            }
        })

        Properties props = ["project": PROJECT_NAME, "directory": directory.getAbsolutePath()]

        DirectoryResourceModelSource.Configuration config = new DirectoryResourceModelSource.Configuration(props);
        final DirectoryResourceModelSource directoryNodesProvider = new DirectoryResourceModelSource(framework);
        directoryNodesProvider.configure(config);

        when:

        final INodeSet nodes = directoryNodesProvider.getNodes();

        then:
        nodes.nodes.size()==1
        nodes.getNode('anode1')!=null
        directoryNodesProvider.modelSourceErrors != null
        directoryNodesProvider.modelSourceErrors == ["Error loading file: "+path+": java.lang.NullPointerException: test npe"]
    }
}
