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

/*
* PoliciesDocument.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Nov 16, 2010 12:01:02 PM
* 
*/
package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.LdapGroup;
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.AuthConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.xml.xpath.*;
import javax.xml.namespace.NamespaceContext;
import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * PoliciesDocument wraps a Document and squeezes lemons.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PoliciesDocument implements PolicyCollection {
    private Document document;
    private File file;
    private ArrayList<String> groupNames;
    private ArrayList<String> projectNames;
    private ArrayList<Policy> policies;
    private long count = Long.MIN_VALUE;
    private static final XPath xpath = XPathFactory.newInstance().newXPath();

    public static final XPathExpression countXpath;
    public static final XPathExpression allPolicies;
    public static final XPathExpression policyByUserName;
    public static final XPathExpression policyByGroup;
    public static final XPathExpression allGroups;
    public static final XPathExpression projectContext;
    public static final XPathExpression allProjectContexts;
    public static final XPathExpression context;
    public static final Set<String> supportedEnvURIs;
    public static final Map<String, String> envURIsToContextAttrs;

    public static final String PROJECT_ATTR = "project";

    static {
        xpath.setNamespaceContext(new NamespaceContext() {

            @SuppressWarnings ("rawtypes")
            public Iterator getPrefixes(String namespaceURI) {
                return null;
            }

            public String getPrefix(String namespaceURI) {
                return null;
            }

            public String getNamespaceURI(String prefix) {
                if (prefix.equals("ldap")) {
                    return PoliciesXml.NS_LDAP;
                } else if (prefix.equals("ActiveDirectory")) {
                    return PoliciesXml.NS_AD;
                } else {
                    return ""; // 1.6 = XMLConstants.NULL_NS_URI;
                }
            }
        });
        try {
            countXpath = xpath.compile("count(//policy)");
            allPolicies = xpath.compile("//policy");
            policyByUserName = xpath.compile("by/user/@username");
            policyByGroup = xpath.compile("by/group/@name | by/group/@ldap:name");
            allGroups = xpath.compile("//by/group/@name | //by/group/@ldap:name");
            context = xpath.compile("context");
            projectContext = xpath.compile("context/@project");
            allProjectContexts = xpath.compile("//policy/context/@project");
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        }

        List<String> uris = Arrays.asList(AuthConstants.PROJECT_URI);
        supportedEnvURIs = Collections.unmodifiableSet(new HashSet<String>(uris));
        HashMap<String, String> ctxts = new HashMap<String, String>();
        ctxts.put(AuthConstants.PROJECT_URI, PROJECT_ATTR);
        envURIsToContextAttrs = Collections.unmodifiableMap(ctxts);
    }

    public PoliciesDocument(final Document document, final File file) {
        this.document = document;
        this.file = file;
    }

    /**
     * @see com.dtolabs.rundeck.core.authorization.providers.PolicyCollection#groupNames()
     */
    public Collection<String> groupNames() throws InvalidCollection {
        if (null != groupNames) {
            return groupNames;
        }
        groupNames = new ArrayList<String>();
        NodeList groups;
        try {
            groups = (NodeList) allGroups.evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new InvalidCollection(e);
        }
        for (int i = 0 ; i < groups.getLength() ; i++) {
            String result = groups.item(i).getNodeValue();
            if (result == null || result.length() <= 0) {
                continue;
            }
            groupNames.add(result);
        }
        return groupNames;
    }

    /**
     */
    public Collection<String> listProjectNames() throws InvalidCollection {
        if (null != projectNames) {
            return projectNames;
        }
        projectNames = new ArrayList<String>();
        NodeList projects;
        try {
            projects = (NodeList) allProjectContexts.evaluate(document, XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            throw new InvalidCollection(e);
        }
        for (int i = 0 ; i < projects.getLength() ; i++) {
            String result = projects.item(i).getTextContent();
            if (result == null || result.length() <= 0) {
                continue;
            }
            projectNames.add(result);
        }
        return projectNames;
    }

    /**
     * @see com.dtolabs.rundeck.core.authorization.providers.PolicyCollection#countPolicies()
     */
    public long countPolicies() throws InvalidCollection {
        if (count != Long.MIN_VALUE) {
            return count;
        }
        try {
            Double xpathCount = (Double) countXpath.evaluate(document, XPathConstants.NUMBER);
            count = xpathCount.longValue();
            return count;
        } catch (XPathExpressionException e) {
            throw new InvalidCollection(e);
        }
    }

    private Collection<Policy> listPolicies() throws XPathExpressionException {
        if (null != policies) {
            return policies;
        }
        policies = new ArrayList<Policy>();

        NodeList policiesToEvaluate = (NodeList) allPolicies.evaluate(document, XPathConstants.NODESET);
        for (int i = 0 ; i < policiesToEvaluate.getLength() ; i++) {

            Node policy = policiesToEvaluate.item(i);
            policies.add(new PolicyNode(policy));
        }
        return policies;
    }

    /**
     * @see com.dtolabs.rundeck.core.authorization.providers.PolicyCollection#matchedContexts(javax.security.auth.Subject,
     *      java.util.Set)
     */
    public Collection<AclContext> matchedContexts(Subject subject, Set<Attribute> environment) throws
        InvalidCollection {
        //select supported env attributes: project
        HashMap<String, String> envMap = createEnvironmentMap(environment);
        try {
            return policyMatcher(subject, listPolicies(), envMap);
        } catch (Exception e) {
            throw new InvalidCollection(e);
        }
    }

    static HashMap<String, String> createEnvironmentMap(Set<Attribute> environment) {
        HashMap<String, String> envMap = new HashMap<String, String>();
        for (final Attribute attribute : environment) {
            if (supportedEnvURIs.contains(attribute.property.toString())) {
                envMap.put(attribute.property.toString(), attribute.value);
            }
        }
        return envMap;
    }

    /**
     * @param subject
     *
     * @return
     *
     * @throws XPathExpressionException
     */
    static Collection<AclContext> policyMatcher(Subject subject, Collection<? extends Policy> policyLister,
                                                final Map<String, String> environment)
        throws InvalidCollection {

        ArrayList<AclContext> matchedContexts = new ArrayList<AclContext>();
        int i = 0;
        for (final Policy policy : policyLister) {

            //require any environment setting to match
            if(!environmentMatches(environment, policy)){
                continue ;
            }

            // What constitutes a match?
            // * The username matches exactly 1 in the context.
            // * 1 subject group matches 1 group.  non disjoint sets.
            //
            // First match stops the search.


            long userMatchStart = System.currentTimeMillis();


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

//                    if(subject.getPrincipals(LdapGroupPrincipal.class).size() > 0) {
//                      //todo check against ldap.
//                    }
            i++;
        }
        return matchedContexts;
    }

    /**
     * Return true if the input environment(URI->string) matches the context's requirements
     */
    static boolean environmentMatches(final Map<String, String> environment, final Policy policy) {
        boolean allowed = true;
        final Map<String, String> policyContext = policy.getEnvironmentContext();
        for (final Map.Entry<String, String> entry : envURIsToContextAttrs.entrySet()) {
            //check all supported environement tests
            final String inputContextValue = environment.get(entry.getKey());
            final String attr = entry.getValue();
            final String contextRequirement = policyContext.get(attr);
            //allow * or null to match all input
            if (null == contextRequirement || "*".equals(contextRequirement)) {
                // match
                continue;
            }
            if (null != inputContextValue) {
                //require regex match on input value
                final Pattern compile = Pattern.compile(contextRequirement);
                final Matcher matcher = compile.matcher(inputContextValue);
                if (!matcher.matches()) {
                    //no match
                    allowed=false;
//                    System.err.println(
//                        "!environment no match: " + attr + " = " + contextRequirement + ": env: " + environment
//                        + policy);
                    break;
                }else{
//                    System.err.println("environment match: " + attr + " = " + contextRequirement + ": env: "
//                                       + environment
//                                       + policy);
                    //match
                }
            } else {
                //null value, but policy requires value match: no match
                allowed=false;
                break;
            }
        }
        return allowed;
    }

    @Override
    public String toString() {
        return "PoliciesDocument{" +
               "file=" + file +
               '}';
    }
}
