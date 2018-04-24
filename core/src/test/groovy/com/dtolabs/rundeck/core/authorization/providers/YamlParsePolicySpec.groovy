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

package com.dtolabs.rundeck.core.authorization.providers

import com.dtolabs.rundeck.core.authorization.providers.yaml.model.YamlPolicyDocConstructor
import org.yaml.snakeyaml.Yaml
import spock.lang.Specification

class YamlParsePolicySpec extends Specification {
    final static String buggyDoc = '''
description: "Users"
context:
 project: *
for:
 resource:
 - equals:
 kind: 'node'
 allow: [read,update,refresh]
 - equals:
 kind: 'job'
 allow: [read,run,kill]
 - equals:
 kind: 'adhoc'
 allow: [read,run,kill]
 - equals:
 kind: 'event'
 allow: [read,create]
 job:
 - match:
 name: '.*'
 allow: [read,run,kill]
 adhoc:
 - match:
 name: '.*'
 allow: [read,run,kill]
 node:
 - match:
 nodename: '.*'
 allow: [read,run,refresh]
by:
 group:
'''

    def "parse invalid document"() {
        given:
        def yaml = new Yaml(new YamlPolicyDocConstructor())
        def all = yaml.loadAll(content)
        def iter = YamlParsePolicy.documentIterable(all.iterator(), null, null);
        when:
        def obj = iter.iterator().next()
        then:
        obj == null
        where:
        content  | _
        buggyDoc | _
    }
}
