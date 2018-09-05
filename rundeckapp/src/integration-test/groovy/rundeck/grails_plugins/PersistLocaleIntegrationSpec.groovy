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

package rundeck.grails_plugins

import grails.gorm.transactions.Rollback
import grails.testing.mixin.integration.Integration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.LocaleResolver
import org.springframework.web.servlet.i18n.CookieLocaleResolver
import spock.lang.Specification


@Integration
@Rollback
class PersistLocaleIntegrationSpec extends Specification {
    @Autowired
    LocaleResolver localeResolver

    def "test locale "() {
        expect: "locale resolver is cookie resolver"
        localeResolver instanceof CookieLocaleResolver
    }
}
