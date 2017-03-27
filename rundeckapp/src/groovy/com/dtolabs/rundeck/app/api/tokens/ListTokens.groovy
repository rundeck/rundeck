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

package com.dtolabs.rundeck.app.api.tokens

import com.dtolabs.rundeck.app.api.marshall.ApiResource
import com.dtolabs.rundeck.app.api.marshall.ElementName
import com.dtolabs.rundeck.app.api.marshall.Ignore
import com.dtolabs.rundeck.app.api.marshall.SubElement
import com.dtolabs.rundeck.app.api.marshall.XmlAttribute

/**
 * @author greg
 * @since 3/23/17
 */
@ApiResource
@ElementName('tokens')
class ListTokens {
    @XmlAttribute
    int count;

    @XmlAttribute
    String user;

    @XmlAttribute
    @Ignore(onlyIfNull = true)
    Boolean allusers;

    @SubElement
    List<Token> tokens;

    ListTokens(final String user, final Boolean allusers, final List<Token> tokens) {
        this.count = tokens.size()
        this.user = user
        this.allusers = allusers ? true : null
        this.tokens = tokens
    }
}

