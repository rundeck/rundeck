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
import com.dtolabs.rundeck.core.authorization.ValidationSet;

import java.io.IOException;
import java.util.Set;

/**
 * @author greg
 * @since 3/20/17
 */
public class LegacyPolicyCollectionFactory implements PolicyCollectionFactory {

    @Override
    public PolicyCollection policiesFromSource(
            final YamlSource source, final Set<Attribute> context, final ValidationSet validation
    ) throws IOException
    {
        return new YamlPolicyCollection(
                source.getIdentity(),
                YamlPolicy.loader(source, validation),
                YamlPolicy.creator(context, validation),
                validation
        );
    }
}
