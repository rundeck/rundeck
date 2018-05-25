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

package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.ValidationSet;
import com.dtolabs.rundeck.core.authorization.providers.yaml.model.ACLPolicyDoc;
import org.yaml.snakeyaml.Yaml;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by greg on 7/17/15.
 */
public interface YamlSource extends Closeable{
    public String getIdentity();
    public Iterable<ACLPolicyDoc> loadAll(Yaml yaml) throws IOException;
    ValidationSet getValidationSet();
}
