/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.Attribute;

import javax.security.auth.Subject;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Policies represent the policies as described in the policies file(s).
 *
 * @author noahcampbell
 */
public class Policies {

    private final List<File> policyFiles = new ArrayList<File>();

    private PoliciesCache cache;


    public Policies(final PoliciesCache cache) {
        this.cache = cache;
    }

    public int count() {
        int count = 0;
        for (PolicyCollection f : cache) {
            count += f.countPolicies();
        }
        return count;
    }

    /**
     * @return Load the policies contained in the root path.
     *
     * @param rootPath file root path
     *
     *
     * @throws PoliciesParseException Thrown when there is a problem parsing a file.
     * @throws IOException  on io error
     */
    public static Policies load(File rootPath) throws IOException, PoliciesParseException {

        Policies p = null;
        try {
            p = new Policies(new PoliciesCache(rootPath));
        } catch (ParserConfigurationException e) {
            throw new PoliciesParseException(e);
        }

        return p;
    }

    public List<AclContext> narrowContext(final Subject subject, final Set<Attribute> environment) {

        List<AclContext> matchedContexts = new ArrayList<AclContext>();
        for (final PolicyCollection f : cache) {
            matchedContexts.addAll(f.matchedContexts(subject, environment));
        }
        return matchedContexts;
    }


    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName());
        builder.append(" [");
        Iterator<File> iter = this.policyFiles.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (iter.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("]");

        return builder.toString();
    }

    /**
     * @return all roles list
     */
    @Deprecated
    public List<String> listAllRoles() {
        List<String> results = new ArrayList<String>();
        for (PolicyCollection f : cache) {
            results.addAll(f.groupNames());

        }

        return results;
    }
}
