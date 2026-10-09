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

import com.dtolabs.rundeck.core.plugins.configuration.ValidationException
import org.rundeck.plugin.scm.git.GitScmSecurityConfig
import spock.lang.Specification

class GitURLValidatorSpec extends Specification {

    def cleanup() {
        System.clearProperty(GitScmSecurityConfig.ALLOWED_SCHEMES)
    }

    def "whitespace is rejected and a normal url is accepted when no allowlist is set"() {
        given:
        def validator = new GitURLValidator()

        when:
        validator.isValid(' https://github.com/org/repo.git')

        then:
        thrown(ValidationException)

        expect:
        validator.isValid('https://github.com/org/repo.git')
        validator.isValid('/tmp/repo.git')
    }

    def "allowlist from the system property rejects file urls"() {
        given:
        System.setProperty(GitScmSecurityConfig.ALLOWED_SCHEMES, 'https,http,ssh')
        def validator = new GitURLValidator()

        when:
        validator.isValid('/tmp/repo.git')

        then:
        def error = thrown(ValidationException)
        error.message == "Git URL scheme 'file' is not allowed."

        expect:
        validator.isValid('git@github.com:org/repo.git')
    }
}
