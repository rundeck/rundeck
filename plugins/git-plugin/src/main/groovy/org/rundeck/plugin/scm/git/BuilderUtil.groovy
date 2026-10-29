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

package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.core.plugins.configuration.Description
import com.dtolabs.rundeck.core.plugins.configuration.Property
import com.dtolabs.rundeck.core.plugins.views.BasicInputView
import com.dtolabs.rundeck.core.plugins.views.BasicInputViewBuilder
import com.dtolabs.rundeck.plugins.util.DescriptionBuilder
import com.dtolabs.rundeck.plugins.util.PropertyBuilder

/**
 * Groovy helpers for builder
 */
class BuilderUtil {

    static Description pluginDescription(@DelegatesTo(DescriptionBuilder) Closure clos) {
        def builder = DescriptionBuilder.builder()
        clos.delegate = builder
        clos.resolveStrategy = Closure.DELEGATE_FIRST
        clos.call()
        return builder.build()
    }

    static BasicInputView inputViewBuilder(String id, @DelegatesTo(BasicInputViewBuilder) Closure clos) {
        def builder = BasicInputViewBuilder.forActionId(id)
        clos.delegate = builder
        clos.resolveStrategy = Closure.DELEGATE_FIRST
        clos.call()
        return builder.build()
    }

    static Property property(@DelegatesTo(PropertyBuilder) Closure clos) {
        def builder = PropertyBuilder.builder()
        clos.delegate = builder
        clos.resolveStrategy = Closure.DELEGATE_FIRST
        clos.call()
        return builder.build()
    }
    static Property property(Property orig,@DelegatesTo(PropertyBuilder) Closure clos) {
        def builder = PropertyBuilder.builder(orig)
        clos.delegate = builder
        clos.resolveStrategy = Closure.DELEGATE_FIRST
        clos.call()
        return builder.build()
    }
}
