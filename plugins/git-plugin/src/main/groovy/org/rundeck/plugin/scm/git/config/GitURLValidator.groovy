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
import org.eclipse.jgit.transport.URIish

class GitURLValidator implements PropertyValidator {
    @Override
    boolean isValid(String value) throws ValidationException {
        if (!value) {
            throw new ValidationException("URL is required.")
        }
        if (value.trim() != value) {
            throw new ValidationException("Leading/trailing whitespace must be removed.")
        }
        try {
            new URIish(value)
        } catch (URISyntaxException e) {
            throw new ValidationException("Not a valid git URL: ${e.message}")
        }
        return true
    }
}
