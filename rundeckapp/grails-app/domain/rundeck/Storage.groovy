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

package rundeck

import com.dtolabs.rundeck.app.support.DomainIndexHelper
import com.fasterxml.jackson.databind.ObjectMapper
import org.rundeck.app.data.model.v1.storage.RundeckStorage
import org.rundeck.storage.api.Path
import org.rundeck.storage.api.PathUtil

import static grails.gorm.hibernate.mapping.MappingBuilder.orm


class Storage implements RundeckStorage{
    String namespace
    String dir
    String name
    /**
     * json encoded metadata
     */
    String jsonData
    /**
     * Unique sha1 of namespace+dir+name to prevent duplicate
     */
    String pathSha
    byte[] data
    Date dateCreated
    Date lastUpdated
    static constraints = {
        namespace(nullable: true, blank: true, size: 0..255)
        jsonData(nullable: true, blank: true)
        data(nullable: true, maxSize: 52_428_800) /* 50MB */
        name(nullable: false, blank: false, maxSize: 1024)
        dir(nullable: true, blank: true, maxSize: 2048)
        pathSha(nullable: false, blank: false, size: 40..40, unique: true)
    }

    private void setupSha() {
        dir = dir ?: ''
        pathSha = ((namespace ?: '') + ':' + getPath().path).encodeAsSHA1()
    }
    def beforeInsert() {
        setupSha()
    }
    def beforeUpdate() {
        setupSha()
    }
    def beforeValidate() {
        setupSha()
    }
    static final mapping = orm {
        cache {
            enabled true
            usage 'nonstrict-read-write'
        }
        property 'data', [type: 'binary']
        property 'dir', [type: 'string']
        property 'jsonData', [type: 'text']
        property 'name', [type: 'string']

        DomainIndexHelper.generate(delegate) {
            index 'STORAGE_IDX_NAMESPACE', ['namespace']
        }
    }
    //ignore fake property 'storageMeta' and 'path' and do not store it
    static transients = ['storageMeta','path']

    @Override
    public Path getPath() {
        return PathUtil.asPath((dir?(dir+'/'):'')+name)
    }
    public void setPath(String path){
        def path1 = PathUtil.asPath(path)
        def parent = PathUtil.parentPath(path1)
        dir= parent?parent.path:''
        name=path1.name
    }

    public Map getStorageMeta() {
        //de-serialize the json
        if (null != jsonData) {
            final ObjectMapper mapper = new ObjectMapper()
            return mapper.readValue(jsonData, Map.class)
        } else {
            return null
        }

    }

    public void setStorageMeta(Map obj) {
        //serialize json and store into field
        if (null != obj) {
            final ObjectMapper mapper = new ObjectMapper()
            jsonData = mapper.writeValueAsString(obj)
        } else {
            jsonData = null
        }
    }

}
