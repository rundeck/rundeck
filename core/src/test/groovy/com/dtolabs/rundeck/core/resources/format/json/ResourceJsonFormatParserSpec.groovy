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

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import spock.lang.Unroll

/**
 * Created by greg on 5/22/15.
 */
class ResourceJsonFormatParserSpec extends Specification {
    static def basicJson='''
{
  "test1": {
    "tags": "aluminum, beans",
    "osFamily": "unix",
    "ssh-key-storage-path": "keys/testkey1.pem",
    "username": "vagrant",
    "osVersion": "10.10.3",
    "osArch": "x86_64",
    "description": "Rundeck server node",
    "hostname": "192.168.33.12",

    "osName": "Mac OS X",
    "rank":"1",
    "doodad":"false"
  },
  "use-nodename-attribute": {
    "tags": "alphabet, soup",
    "osFamily": "unix",
    "ssh-key-storage-path": "keys/testkey1.pem",
    "username": "vagrant",
    "osVersion": "10.10.3",
    "osArch": "x86_64",
    "description": "Rundeck server node",
    "hostname": "192.168.33.12",
    "nodename": "test2",
    "osName": "Mac OS X",
    "rank":"2",
    "doodad":"true",
    "meddlesome":null
  }
}
'''
    static def basicArrayJson='''
[
   {
    "tags": "aluminum, beans",
    "osFamily": "unix",
    "ssh-key-storage-path": "keys/testkey1.pem",
    "username": "vagrant",
    "osVersion": "10.10.3",
    "osArch": "x86_64",
    "description": "Rundeck server node",
    "hostname": "192.168.33.12",
    "nodename": "test1",
    "osName": "Mac OS X",
    "rank":"1",
    "doodad":"false"
  },
  {
    "tags": "alphabet, soup",
    "osFamily": "unix",
    "ssh-key-storage-path": "keys/testkey1.pem",
    "username": "vagrant",
    "osVersion": "10.10.3",
    "osArch": "x86_64",
    "description": "Rundeck server node",
    "hostname": "192.168.33.12",
    "nodename": "test2",
    "osName": "Mac OS X",
    "meddlesome": null,
    "rank":"2",
    "doodad":"true"
  }
]
'''
    /**
     * has integer/boolean in the data
     */
    static def basicJsonScalars='''
{
  "test1": {
    "tags": "aluminum, beans",
    "osFamily": "unix",
    "ssh-key-storage-path": "keys/testkey1.pem",
    "username": "vagrant",
    "osVersion": "10.10.3",
    "osArch": "x86_64",
    "description": "Rundeck server node",
    "hostname": "192.168.33.12",
    "nodename": "test1",
    "osName": "Mac OS X",
    "rank":1,
    "doodad":false
  },
  "test2": {
    "tags": "alphabet, soup",
    "osFamily": "unix",
    "ssh-key-storage-path": "keys/testkey1.pem",
    "username": "vagrant",
    "osVersion": "10.10.3",
    "osArch": "x86_64",
    "description": "Rundeck server node",
    "hostname": "192.168.33.12",
    "nodename": "test2",
    "osName": "Mac OS X",
    "meddlesome": null,
    "rank":2,
    "doodad":true
  }
}
'''
    static def basicTagsArrayJson='''
{
  "test1": {
    "tags": [ "aluminum", "beans" ],
    "osFamily": "unix",
    "ssh-key-storage-path": "keys/testkey1.pem",
    "username": "vagrant",
    "osVersion": "10.10.3",
    "osArch": "x86_64",
    "description": "Rundeck server node",
    "hostname": "192.168.33.12",
    "nodename": "test1",
    "osName": "Mac OS X",
    "rank":"1",
    "doodad":"false"
  },
  "test2": {
    "tags": ["alphabet", "soup"],
    "osFamily": "unix",
    "ssh-key-storage-path": "keys/testkey1.pem",
    "username": "vagrant",
    "osVersion": "10.10.3",
    "osArch": "x86_64",
    "description": "Rundeck server node",
    "hostname": "192.168.33.12",
    "nodename": "test2",
    "osName": "Mac OS X",
    "meddlesome": null,
    "rank":"2",
    "doodad":"true"
  }
}
'''


    @Unroll
    def "parse basic"(String json, List nodenames){
        given:
        def parser = new ResourceJsonFormatParser()
        def result=parser.parseDocument(new ByteArrayInputStream(json.getBytes()))
        def node1=result.getNode("test1")
        def node2=result.getNode("test2")

        expect:
        result!=null
        result.nodes!=null
        result.nodes.size()==2
        result.nodeNames.size()==2
        result.nodeNames.containsAll(nodenames)
        node1!=null
        node1.getNodename()=='test1'
        node1.getAttributes()==[
                "tags": "aluminum, beans",
                "osFamily": "unix",
                "ssh-key-storage-path": "keys/testkey1.pem",
                "username": "vagrant",
                "osVersion": "10.10.3",
                "osArch": "x86_64",
                "description": "Rundeck server node",
                "hostname": "192.168.33.12",
                "nodename": "test1",
                "osName": "Mac OS X",
                "rank":"1",
                "doodad":"false"]

        node2!=null

        node2.getNodename()=='test2'
        node2.getAttributes()==[
                "tags": "alphabet, soup",
                "osFamily": "unix",
                "ssh-key-storage-path": "keys/testkey1.pem",
                "username": "vagrant",
                "osVersion": "10.10.3",
                "osArch": "x86_64",
                "description": "Rundeck server node",
                "hostname": "192.168.33.12",
                "nodename": "test2",
                "osName": "Mac OS X",
                "rank":"2",
                "doodad":"true"]

        where:
        json               | nodenames
        basicJson          | ['test1', 'test2']
        basicArrayJson     | ['test1', 'test2']
        basicTagsArrayJson | ['test1', 'test2']
        basicJsonScalars   | ['test1', 'test2']

    }
}
