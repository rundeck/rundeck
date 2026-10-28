/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.plugin.scm.git.config

import com.dtolabs.rundeck.core.plugins.configuration.PropertyValidator
import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import com.dtolabs.rundeck.plugins.scm.ScmPluginException
import groovy.transform.CompileStatic
import org.rundeck.plugin.scm.git.GitScmSecurityConfig
import org.rundeck.plugin.scm.git.GitUrlPolicy

/**
 * Rejects whitespace and, when {@code rundeck.scm.git.allowedSchemes} is set, schemes outside that list.
 * Framework properties are enforced again when the remote connection is opened.
 */
@CompileStatic
class GitURLValidator implements PropertyValidator {
    @Override
    boolean isValid(String value) throws ValidationException {
        if (value.trim() != value) {
            throw new ValidationException("Leading/trailing whitespace must be removed.")
        }
        try {
            GitUrlPolicy.assertAllowed(value, GitScmSecurityConfig.resolve(null))
        } catch (ScmPluginException e) {
            throw new ValidationException(e.message)
        }
        return true
    }
}
