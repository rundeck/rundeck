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
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import com.dtolabs.rundeck.core.authorization.Attribute;

/**
 * @author noahcampbell
 */
public class PoliciesYaml implements PolicyCollection {
    static Logger logger = Logger.getLogger(PoliciesYaml.class.getName());
    private final Set<YamlPolicy> all = new HashSet<YamlPolicy>();
    File file;


    public PoliciesYaml(final File file) throws IOException {
        final Yaml yaml = new Yaml();
        this.file = file;
        final FileInputStream stream = new FileInputStream(this.file);
        int index=1;
        try {
            for (Object yamlDoc : yaml.loadAll(stream)) {
                final Object yamlDoc1 = yamlDoc;
                if (yamlDoc1 instanceof Map) {
                    all.add(new YamlPolicy((Map) yamlDoc1, file, index));
                }
                index++;
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
        return policyMatcher(subject, all, environment, file);

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
                                                final Set<Attribute> environment, final File file)
        throws InvalidCollection {
        final ArrayList<AclContext> matchedContexts = new ArrayList<AclContext>();
        int i = 0;
        Set<Username> userPrincipals = subject.getPrincipals(Username.class);
        Set<String> usernamePrincipals = new HashSet<String>();
        if (userPrincipals.size() > 0) {
            for (Username username : userPrincipals) {
                usernamePrincipals.add(username.getName());
            }
        }
        Set<Group> groupPrincipals = subject.getPrincipals(Group.class);
        Set<Object> groupNames = new HashSet<Object>();
        if (groupPrincipals.size() > 0) {
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
        }
        for (final Policy policy : policyLister) {
            long userMatchStart = System.currentTimeMillis();

            if(null!=policy.getEnvironment()){
                final EnvironmentalContext environment1 = policy.getEnvironment();
                if(!environment1.isValid()) {
                    logger.warn(policy.toString()+ ": Context section not valid: " + environment1.toString());
                }
                if(!environment1.matches(environment)){
                    if(logger.isDebugEnabled()){
                        logger.debug(policy.toString() + ": environment not matched: " + environment1.toString());
                    }
                    continue;
                }
            }else if (null != environment && environment.size() > 0) {
                logger.debug(policy.toString() + ": empty environment not matched");
                continue;
            }
            

            if (usernamePrincipals.size() > 0) {
                Set<String> policyUsers = policy.getUsernames();
                if (!Collections.disjoint(policyUsers, usernamePrincipals)) {
                    matchedContexts.add(policy.getContext());
                    continue;
                }else if(policyUsers.size()>0){
                    if (logger.isDebugEnabled()) {
                        logger.debug(policy.toString() + ": username not matched: "+ policyUsers);
                    }
                }
            }


            if (groupNames.size() > 0) {
                // no username matched, check groups.
                Set<Object> policyGroups = policy.getGroups();
                if (!Collections.disjoint(policyGroups, groupNames)) {
                    matchedContexts.add(policy.getContext());
                    continue;
                }else if(policyGroups.size()>0){
                    if(logger.isDebugEnabled()){
                        logger.debug(policy.toString() + ": group not matched: " + policyGroups);
                    }
                }
            }

            i++;
        }
        logger.debug(file.getAbsolutePath() + ": matched contexts: " + matchedContexts.size());
        return matchedContexts;
    }
}
