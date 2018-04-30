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

package com.dtolabs.rundeck.app.support

import grails.validation.Validateable

/**
 * @author greg
 * @since 9/19/17
 */
class AclFile implements Validateable {
    String name
    String id

    static constraints = {
        name(nullable: true, matches: /^(?!\.\.(\/|$))[a-zA-Z0-9,\.+_-][a-zA-Z0-9,\.+_-]*$/)
        id(nullable: true, matches: /^(?!\.\.(\/|$))[a-zA-Z0-9,\.+_-][a-zA-Z0-9,\.+_-]*\.aclpolicy$/)
    }

    /**
     * @return name from the id
     */
    String idToName() {
        idToName(id)
    }

    /**
     * Convert a *.aclpolicy id into just the name
     * @param id file name with extension
     * @return base name
     */
    static String idToName(String id) {
        id?.replaceFirst(/\.aclpolicy$/, '')
    }

    /**
     * @return name with extension
     */
    String nameToId() {
        name ? (name + '.aclpolicy') : null
    }

    /**
     * Return the id if set, or generate the id from the name
     * @return
     */
    String createId() {
        id ?: nameToId()
    }
    /**
     * Return the name if set, or get the name from the id
     * @return
     */
    String createName() {
        name ?: idToName()
    }
}
