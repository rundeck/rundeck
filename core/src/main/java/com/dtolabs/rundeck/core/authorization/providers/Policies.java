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

import com.dtolabs.rundeck.core.authorization.*;

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
public class Policies implements AclRuleSetSource{

    private Iterable<PolicyCollection> cache;
    private ValidationSet validation;

    public Policies(final Iterable<PolicyCollection> cache, final ValidationSet validationSet) {
        this.validation=validationSet;
        this.cache = cache;
    }
    public Policies(final Iterable<PolicyCollection> cache) {
        this(cache, null);
    }

    public int count() {
        int count = 0;
        for (PolicyCollection f : cache) {
            count += f.countPolicies();
        }
        return count;
    }

    @Override
    public AclRuleSet getRuleSet() {
        Set<AclRule> set = new HashSet<>();
        for (final PolicyCollection f : cache) {
            set.addAll(f.getRuleSet().getRules());
        }
        return new AclRuleSetImpl(set);
    }

    /**
     * @return Load the policies contained in the root path.
     *
     * @param rootPath file root path
     *
     *
     */
    public static Policies load(File rootPath)  {
        return new Policies(PoliciesCache.fromDir(rootPath));
    }

    /**
     * @return Load the policies contained in the root path.
     *
     * @param rootPath file root path
     *
     *
     */
    public static Policies load(File rootPath, final Set<Attribute> forcedContext)  {
        return new Policies(PoliciesCache.fromDir(rootPath));
    }
    /**
     * @return Load the policies contained in the root path.
     *
     * @param singleFile single file
     *
     *
     */
    public static Policies loadFile(File singleFile)  {
        return new Policies(PoliciesCache.fromFile(singleFile));
    }

    public List<AclContext> narrowContext(final Subject subject, final Set<Attribute> environment) {

        List<AclContext> matchedContexts = new ArrayList<AclContext>();
        for (final PolicyCollection f : cache) {
            matchedContexts.addAll(f.matchedContexts(subject, environment));
        }
        return matchedContexts;
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
