/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Validation;
import com.dtolabs.rundeck.core.authorization.ValidationSet;

import java.io.IOException;
import java.util.Set;

/**
 * @author greg
 * @since 3/20/17
 */
public interface PolicyCollectionFactory {
    default public PolicyCollection policiesFromSource(final YamlSource source) throws IOException{
        return policiesFromSource(source, null, null);
    }

    /**
     * Load policies from a source
     *
     * @param source  source
     * @param context Context to require for all policies parsed
     *
     * @return policies
     *
     * @throws IOException
     */
    public PolicyCollection policiesFromSource(
            final YamlSource source,
            final Set<Attribute> context,
            final ValidationSet validation
    )
            throws IOException;

    default public PolicyCollection policiesFromSource(
            final YamlSource source,
            final Set<Attribute> context
    )
            throws IOException{
        return policiesFromSource(source, context, null);
    }

    default Validation validate(
            ValidationSet validation,
            final Iterable<CacheableYamlSource> sources,
            final Set<Attribute> forcedContext
    )
    {
        for (CacheableYamlSource source : sources) {
            try {
                PolicyCollection yamlPolicyCollection = policiesFromSource(
                        source,
                        forcedContext,
                        validation
                );
            } catch (Exception  e1) {
                e1.printStackTrace();
                validation.addError(source.getIdentity(), e1.getMessage());
            }
        }
        validation.complete();
        return validation;
    }
}
