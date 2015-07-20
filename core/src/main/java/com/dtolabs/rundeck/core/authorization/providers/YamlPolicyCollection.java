/**
 *
 */
package com.dtolabs.rundeck.core.authorization.providers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.LdapGroup;
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.AclRule;
import com.dtolabs.rundeck.core.authorization.AclRuleSet;
import com.dtolabs.rundeck.core.authorization.AclRuleSetImpl;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

import com.dtolabs.rundeck.core.authorization.Attribute;

/**
 * Stores a collection of policies, read in from a source.
 * @author noahcampbell
 */
public class YamlPolicyCollection implements PolicyCollection {
    static Logger logger = Logger.getLogger(YamlPolicyCollection.class.getName());
    private final Set<YamlPolicy> all = new HashSet<>();
    private final Set<AclRule> ruleSet = new HashSet<>();
    YamlSource source;

    /**
     * Create from a source
     * @param source source
     * @throws IOException
     */
    public YamlPolicyCollection(final YamlSource source) throws IOException {
        this.source=source;
        load(source);
    }

    @Override
    public AclRuleSet getRuleSet() {
        return new AclRuleSetImpl(ruleSet);
    }

    /**
     * load yaml stream as sequence of policy documents
     * @param source content source
     * @throws IOException
     */
    private void load(final YamlSource source) throws IOException {
        final Yaml yaml = new Yaml();
        int index = 1;
        try(final YamlSource source1=source) {
            for (Object yamlDoc : source1.loadAll(yaml)) {
                if (!(yamlDoc instanceof Map)) {
                    logger.error(
                            "ERROR parsing a policy in: " +
                            source.getIdentity() +
                            "[" +
                            index +
                            "]. Expected a policy document Map, but found: " + yamlDoc.getClass().getName()
                    );
                }
                try {
                    YamlPolicy yamlPolicy = new YamlPolicy((Map) yamlDoc, source.getIdentity(), index);
                    all.add(yamlPolicy);
                    ruleSet.addAll(yamlPolicy.getRuleSet().getRules());
                } catch (YamlPolicy.AclPolicySyntaxException e) {
                    logger.error(
                            "ERROR parsing a policy in file: " +
                            source.getIdentity() +
                            "[" +
                            index +
                            "]. Reason: " +
                            e.getMessage()
                    );
                    logger.debug(
                            "ERROR parsing a policy in file: " +
                            source.getIdentity() +
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

    public Collection<String> groupNames()  {
        List<String> groups = new ArrayList<String>();
        for (YamlPolicy policy : all) {
            for (String policyGroup : policy.getGroups()) {
                groups.add(policyGroup);
            }
        }
        return groups;
    }

    public long countPolicies()  {
        return all.size();
    }

    public Collection<AclContext> matchedContexts(final Subject subject, final Set<Attribute> environment) {
        return policyMatcher(subject, all, environment, source.getIdentity());
    }

    /**
     * @param environment    environment
     * @param policyLister   collection
     * @param subject        subject
     * @param sourceIdentity identity of source
     *
     * @return contexts
     */
    static Collection<AclContext> policyMatcher(
            final Subject subject,
            final Collection<? extends Policy> policyLister,
            final Set<Attribute> environment,
            final String sourceIdentity
    )
    {
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

            if (null != policy.getEnvironment()) {
                final EnvironmentalContext environment1 = policy.getEnvironment();
                if (!environment1.isValid()) {
                    logger.warn(policy.toString() + ": Context section not valid: " + environment1.toString());
                }
                if (!environment1.matches(environment)) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(policy.toString() + ": environment not matched: " + environment1.toString());
                    }
                    continue;
                }
            } else if (null != environment && environment.size() > 0) {
                logger.debug(policy.toString() + ": empty environment not matched");
                continue;
            }


            if (usernamePrincipals.size() > 0) {
                Set<String> policyUsers = policy.getUsernames();
                if (!Collections.disjoint(policyUsers, usernamePrincipals)
                    || matchesAnyPatterns(usernamePrincipals, policy.getUsernamePatterns())) {
                    matchedContexts.add(policy.getContext());
                    continue;
                } else if (policyUsers.size() > 0) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(policy.toString() + ": username not matched: " + policyUsers);
                    }
                }
            }


            if (groupNames.size() > 0) {
                // no username matched, check groups.
                Set<String> policyGroups = policy.getGroups();
                if (!Collections.disjoint(policyGroups, groupNames)
                    || matchesAnyPatterns(groupNames, policy.getGroupPatterns())) {
                    matchedContexts.add(policy.getContext());
                    continue;
                } else if (policyGroups.size() > 0) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(policy.toString() + ": group not matched: " + policyGroups);
                    }
                }
            }

            i++;
        }
        logger.debug(sourceIdentity + ": matched contexts: " + matchedContexts.size());
        return matchedContexts;
    }

    static boolean matchesAnyPatterns(Set<?> groupNames, Set<Pattern> groupPatterns) {
        for (Pattern groupPattern : groupPatterns) {
            for (Object groupName : groupNames) {
                if(groupPattern.matcher(groupName.toString()).matches()) {
                    return true;
                }
            }
        }
        return false;
    }
}
