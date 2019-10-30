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

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.storage.AuthStorageTree
import com.dtolabs.rundeck.core.storage.BaseStorage
import com.dtolabs.rundeck.core.storage.IBaseStorage
import com.dtolabs.rundeck.core.storage.StorageUtil
import com.dtolabs.rundeck.core.storage.keys.KeyStorageTree
import com.dtolabs.rundeck.core.storage.keys.KeyStorageUtil
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware

/**
 * Service layer access to the authorized storage
 */
class StorageService extends BaseStorage {
    def rundeckAuthStorageServiceManager

    @Override
    protected AuthStorageTree getServiceTree() {
        return rundeckAuthStorageServiceManager.getServiceTree()
    }

    /**
     * Return a tree using the authorization context
     * @param ctx auth context
     * @return StorageTree
     */
    def KeyStorageTree storageTreeWithContext(AuthContext ctx) {
        KeyStorageUtil.keyStorageWrapper StorageUtil.resolvedTree(ctx, rundeckAuthStorageServiceManager.getServiceTree())
    }
}

class AuthStorageServiceManager extends BaseStorage implements IBaseStorage, ApplicationContextAware {
    AuthStorageTree authRundeckStorageTree
    ApplicationContext applicationContext

    @Override
    protected AuthStorageTree getServiceTree() {
        if(!authRundeckStorageTree){
            authRundeckStorageTree = applicationContext.getBean('authRundeckStorageTree')
        }
        return authRundeckStorageTree
    }
}
