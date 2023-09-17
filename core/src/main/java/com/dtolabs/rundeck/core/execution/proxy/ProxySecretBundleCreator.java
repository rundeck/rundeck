/*
 * Copyright 2021 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.core.execution.proxy;

import com.dtolabs.rundeck.core.common.INodeEntry;
import com.dtolabs.rundeck.core.execution.ExecutionContext;
import org.rundeck.app.spi.Services;

import java.util.List;
import java.util.Map;

public interface ProxySecretBundleCreator {
    default SecretBundle prepareSecretBundle(ExecutionContext context, INodeEntry node){
        return null;
    }

    default SecretBundle prepareSecretBundleWorkflowStep(ExecutionContext context, Map<String, Object> configuration){
        return null;
    }

    default SecretBundle prepareSecretBundleWorkflowNodeStep(ExecutionContext context, INodeEntry node, Map<String, Object> configuration){
        return null;
    }

    default SecretBundle prepareSecretBundleResourceModel(Services services, Map<String, Object> configuration){
        return null;
    }


    default List<String> listSecretsPath(ExecutionContext context, INodeEntry node){
        return null;
    }

    default List<String> listSecretsPathWorkflowNodeStep(ExecutionContext context, INodeEntry node, Map<String, Object> configuration){
        return null;
    }

    default List<String> listSecretsPathWorkflowStep(ExecutionContext context, Map<String, Object> configuration){
        return null;
    }

    default List<String> listSecretsPathResourceModel(Map<String, Object> configuration){
        return null;
    }

}
