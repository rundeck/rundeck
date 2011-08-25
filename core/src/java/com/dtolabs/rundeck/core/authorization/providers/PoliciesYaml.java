/**
 *
 */
package com.dtolabs.rundeck.core.authorization.providers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import javax.security.auth.Subject;

import org.yaml.snakeyaml.Yaml;

import com.dtolabs.rundeck.core.authorization.Attribute;

/**
 * @author noahcampbell
 */
public class PoliciesYaml implements PolicyCollection {

    private final Set<YamlPolicy> all = new HashSet<YamlPolicy>();


    public PoliciesYaml(final File file) throws IOException {
        final Yaml yaml = new Yaml();
        final FileInputStream stream = new FileInputStream(file);
        try {
            for (Object yamlDoc : yaml.loadAll(stream)) {
                final Object yamlDoc1 = yamlDoc;
                if(yamlDoc1 instanceof Map) {
                    all.add(new YamlPolicy((Map) yamlDoc1));
                }
            }
        } finally {
            stream.close();
        }
    }

    public Collection<String> groupNames() throws InvalidCollection {
        List<String> groups = new ArrayList<String>();
        for (YamlPolicy policy : all) {
            for (Object policyGroup : policy.getGroups()) {
                groups.add(policyGroup.toString());
            }
        }
        return groups;
    }

    public long countPolicies() throws InvalidCollection {
        return all.size();
    }

    public Collection<AclContext> matchedContexts(Subject subject, Set<Attribute> environment)
        throws InvalidCollection {
        return PoliciesDocument.policyMatcher(subject, all);

    }
}
