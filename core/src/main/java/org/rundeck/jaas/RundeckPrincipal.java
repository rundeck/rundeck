/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.jaas;

import java.io.Serializable;
import java.security.Principal;

/**
 * Principal implementation for authenticated users.
 * Replaces org.eclipse.jetty.jaas.JAASPrincipal
 */
public class RundeckPrincipal implements Principal, Serializable {
    private static final long serialVersionUID = 1L;
    private final String name;

    public RundeckPrincipal(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        // Grails 7: Changed to check class type to prevent deduplication with RundeckRole
        // Previously only checked name, which caused RundeckRole[admin] to be deduplicated
        // with RundeckPrincipal[admin] when both were added to the Subject's Principal Set
        if (!(obj instanceof RundeckPrincipal)) return false;
        RundeckPrincipal other = (RundeckPrincipal) obj;
        return name != null ? name.equals(other.getName()) : other.getName() == null;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "RundeckPrincipal[" + name + "]";
    }
}

