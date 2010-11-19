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


/**
 * PoliciesDocument wraps a Document and
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PoliciesDocument {
    private Document document;
    private File file;
    private ArrayList<String> groupNames;
    private ArrayList<Policy> policies;
    private Double count=null;
    private static final XPath xpath = XPathFactory.newInstance().newXPath();

    public static final XPathExpression countXpath;
    public static final XPathExpression allPolicies;
    public static final XPathExpression policyByUserName;
    public static final XPathExpression policyByGroup;
    public static final XPathExpression allGroups;

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
                    return Policies.NS_LDAP;
                } else if (prefix.equals("ActiveDirectory")) {
                    return Policies.NS_AD;
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
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        }

    }

    public PoliciesDocument(final Document document, final File file) {
        this.document = document;
        this.file=file;
    }

    public Collection<String> groupNames() throws XPathExpressionException {
        if (null != groupNames) {
            return groupNames;
        }
        groupNames = new ArrayList<String>();
        NodeList groups = (NodeList) allGroups.evaluate(document, XPathConstants.NODESET);
        for (int i = 0 ; i < groups.getLength() ; i++) {
            String result = groups.item(i).getNodeValue();
            if (result == null || result.length() <= 0) {
                continue;
            }
            groupNames.add(result);
        }
        return groupNames;
    }

    public Double countPolicies() throws XPathExpressionException {
        if (null != count) {
            return count;
        }
        count = (Double) countXpath.evaluate(document, XPathConstants.NUMBER);
        return count;
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

    public Collection<Policies.Context> matchedContexts(Subject subject, Set<Attribute> environment) throws
        XPathExpressionException {
        ArrayList<Policies.Context> matchedContexts = new ArrayList<Policies.Context>();
        int i = 0;
        for (final Policy policy : listPolicies()) {


            // What constitutes a match?
            // * The username matches exactly 1 in the context.
            // * 1 subject group matches 1 group.  non disjoint sets.
            //
            // First match stops the search.

            // TODO: time of day check.

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
                    break;
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

    @Override
    public String toString() {
        return "PoliciesDocument{" +
               "file=" + file +
               '}';
    }
}
