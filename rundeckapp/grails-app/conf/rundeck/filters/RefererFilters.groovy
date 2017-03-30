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

package rundeck.filters

import java.util.regex.Pattern

class RefererFilters {
    def dependsOn = [ApiRequestFilters]
    def filters = {
        checkReferer(controller: 'user', action: 'login', invert: true) {
            before = {
                def csrf = grailsApplication.config.rundeck.security.csrf
                def urlString = (grailsApplication.config.grails.serverURL).toString()
                def validRefererPrefix = "^" + Pattern.quote(urlString).replace("^https://", "^https?://")
                def referer = request.getHeader('Referer')
                if (csrf && csrf != 'NONE') {
                    def isvalidReferer = referer && referer =~ validRefererPrefix
                    if (csrf == 'POST') {
                        if (request.method.toUpperCase() == "POST") {
                            // referer must match serverURL, optionally https

                            if(!isvalidReferer) {
                                System.err.println("${request.method}: invalid referer")
                            }
                            return isvalidReferer
                        }
                    } else if (csrf == '*') {
                        if(!isvalidReferer) {
                            System.err.println("${request.method}: invalid referer")
                        }
                        return isvalidReferer
                    }
                }

            }
        }
    }
}