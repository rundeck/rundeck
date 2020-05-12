/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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
package com.dtolabs.rundeck.app.gui

import grails.web.mapping.LinkGenerator
import org.rundeck.app.gui.JobListLinkHandler
import org.springframework.beans.factory.annotation.Autowired


class GroupedJobListLinkHandler implements JobListLinkHandler {

    public static final String NAME = "grouped"

    @Autowired
    LinkGenerator linkGenerator

    @Override
    String getName() {
        return NAME
    }

    @Override
    Map generateRedirectMap(final Map redirectParams) {
        return [controller:'menu',action:'jobs', params: [project: redirectParams]]
    }

    @Override
    String generateLinkToJobListAction(final String project) {
        return linkGenerator.link(generateRedirectMap([project:project]))
    }
}
