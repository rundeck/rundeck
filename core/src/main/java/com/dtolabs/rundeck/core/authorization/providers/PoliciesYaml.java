/**
 *
 */
package com.dtolabs.rundeck.core.authorization.providers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.LdapGroup;
import com.dtolabs.rundeck.core.authentication.Username;
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
                if (yamlDoc1 instanceof Map) {
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

    public Collection<AclContext> matchedContexts(final Subject subject, final Set<Attribute> environment)
        throws InvalidCollection {
        return policyMatcher(subject, all, environment);

    }

    /**
     * @param subject
     *
     * @return
     *
     * @throws javax.xml.xpath.XPathExpressionException
     *
     */
    static Collection<AclContext> policyMatcher(final Subject subject, final Collection<? extends Policy> policyLister,
                                                final Set<Attribute> environment)
        throws InvalidCollection {
        final ArrayList<AclContext> matchedContexts = new ArrayList<AclContext>();
        int i = 0;
        for (final Policy policy : policyLister) {
            long userMatchStart = System.currentTimeMillis();


            //todo: evaluate environment and context values for the policies
            if(null!=policy.getEnvironment()){
                final EnvironmentalContext environment1 = policy.getEnvironment();
                if(!environment1.matches(environment)){
                    continue;
                }
            }else if (null != environment && environment.size() > 0) {
                continue;
            }
            

            Set<Username> userPrincipals = subject.getPrincipals(Username.class);
            if (userPrincipals.size() > 0) {
                Set<String> policyUsers = policy.getUsernames();
                Set<String> usernamePrincipals = new HashSet<String>();
                for (Username username : userPrincipals) {
                    usernamePrincipals.add(username.getName());
                }

                if (!Collections.disjoint(policyUsers, usernamePrincipals)) {
                    matchedContexts.add(policy.getContext());
                    continue;
                }
            }


            Set<Group> groupPrincipals = subject.getPrincipals(Group.class);
            if (groupPrincipals.size() > 0) {
                // no username matched, check groups.
                long groupCollectStart = System.currentTimeMillis();

                Set<Object> policyGroups = policy.getGroups();
                Set<Object> groupNames = new HashSet<Object>();
                for (Group groupPrincipal : groupPrincipals) {
                    if (groupPrincipal instanceof LdapGroup) {
                        try {
                            groupNames.add(new LdapName(groupPrincipal.getName()));
                        } catch (InvalidNameException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    } else {
                        groupNames.add(groupPrincipal.getName());
                    }
                }

                long collectDuration = System.currentTimeMillis() - groupCollectStart;
                if (!Collections.disjoint(policyGroups, groupNames)) {
                    matchedContexts.add(policy.getContext());
                    continue;
                }
            }

            i++;
        }
        return matchedContexts;
    }
}
