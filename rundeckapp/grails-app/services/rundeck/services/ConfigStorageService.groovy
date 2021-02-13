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


import com.dtolabs.rundeck.core.storage.StorageManager
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.core.storage.StorageTreeFactory
import grails.gorm.transactions.Transactional
import groovy.transform.CompileStatic

/**
 * Interact with configuration storage
 */
@Transactional
@CompileStatic
class ConfigStorageService {
    StorageTree rundeckConfigStorageTree
    @Delegate
    StorageManager rundeckConfigStorageManager


    boolean hasFixIndicator(String name) {
        existsFileResource(getSystemFixIndicatorPath(name))
    }

    String getSystemFixIndicatorPath(String name) {
        "sys/fix/$name"
    }

    /**
     * Provides non-authorizing subtree for the given subpath
     * @param subpath
     * @return
     */
    StorageTree storageTreeSubpath(String subpath) {
        StorageTreeFactory.subTree(
            rundeckConfigStorageTree,
            subpath
        )
    }
}
