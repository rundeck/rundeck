/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

/**
 *
 */
package com.dtolabs.rundeck.core.authorization.providers;

import java.io.Closeable;
import java.io.IOException;
import java.util.*;

import com.dtolabs.rundeck.core.authorization.*;
import org.apache.log4j.Logger;

/**
 * Stores a collection of policies, read in from a source.
 * @author noahcampbell
 */
public class YamlPolicyCollection implements PolicyCollection {
    static Logger logger = Logger.getLogger(YamlPolicyCollection.class.getName());
    private final Set<Policy> all = new HashSet<>();
    private final Set<AclRule> ruleSet = new HashSet<>();
    String identity;
    final ValidationSet validation;
    /**
     * Create from a source
     *
     * @param identity source identity string
     *
     * @throws IOException
     */
    public YamlPolicyCollection(
            final String identity,
            YamlSourceLoader loader,
            YamlPolicyCreator creator,
            final ValidationSet validation
    )
            throws IOException
    {
        this.identity = identity;
        this.validation=validation;
        load(loader, creator);
    }

    @Override
    public AclRuleSet getRuleSet() {
        return new AclRuleSetImpl(ruleSet);
    }

    static interface YamlSourceLoader<T> extends Closeable {
        Iterable<T> loadAll() throws IOException;
    }

    static interface YamlPolicyCreator<T> {
        Policy createYamlPolicy(
                final T policyInput,
                final String sourceIdent,
                final int sourceIndex
        ) throws AclPolicySyntaxException;
    }

    /**
     * load yaml stream as sequence of policy documents
     *
     * @throws IOException
     */
    private <T> void load(YamlSourceLoader<T> loader, YamlPolicyCreator<T> creator)
            throws IOException
    {
        int index = 1;
        try (final YamlSourceLoader<T> loader1 = loader) {
            for (T yamlDoc : loader1.loadAll()) {
                String ident = identity + "[" + index + "]";
                if (null == yamlDoc) {
                    continue;
                }
                try {
                    Policy yamlPolicy = creator.createYamlPolicy(
                            yamlDoc,
                            identity + "[" + index + "]",
                            index
                    );
                    all.add(yamlPolicy);
                    ruleSet.addAll(yamlPolicy.getRuleSet().getRules());
                } catch (AclPolicySyntaxException e) {
                    validationError(ident, e.getMessage());
                    logger.debug(
                            "ERROR parsing a policy in file: " +
                            identity +
                            "[" +
                            index +
                            "]. Reason: " +
                            e.getMessage(), e
                    );
                }
                index++;
            }

        }
    }

    private void validationError(final String ident, final String reason) {
        if(null!=validation) {
            validation.addError(ident, reason);
        }
    }

    public Collection<String> groupNames()  {
        List<String> groups = new ArrayList<String>();
        for (Policy policy : all) {
            for (String policyGroup : policy.getGroups()) {
                groups.add(policyGroup);
            }
        }
        return groups;
    }

    public long countPolicies()  {
        return all.size();
    }

}
