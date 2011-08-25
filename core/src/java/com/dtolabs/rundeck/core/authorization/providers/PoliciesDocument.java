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
import com.dtolabs.rundeck.core.authorization.Explanation;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.xml.xpath.*;
import javax.xml.namespace.NamespaceContext;
import java.io.File;
import java.util.*;
import java.util.regex.Pattern;


/**
 * PoliciesDocument implements a policy collection defined by a XML Document.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PoliciesDocument implements PolicyCollection {

    static final String NS_AD = "http://dtolabs.com/rundeck/activedirectory";
    static final String NS_LDAP = "http://dtolabs.com/rundeck/ldap";
    private Document document;
    private File file;
    private ArrayList<String> groupNames;
    private ArrayList<Policy> policies;
    private long count = Long.MIN_VALUE;
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
                    return NS_LDAP;
                } else if (prefix.equals("ActiveDirectory")) {
                    return NS_AD;
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

    final static private Map<String, XPathExpression> commandFilterCache = new HashMap<String, XPathExpression>();
    public PoliciesDocument(final Document document, final File file) {
        this.document = document;
        this.file=file;
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
     * @see com.dtolabs.rundeck.core.authorization.providers.PolicyCollection#matchedContexts(javax.security.auth.Subject, java.util.Set)
     */
    public Collection<AclContext> matchedContexts(Subject subject, Set<Attribute> environment) throws
        InvalidCollection {
        try {
            return policyMatcher(subject, listPolicies());
        } catch (Exception e) {
            throw new InvalidCollection(e);
        }
    }

    /**
     * @param subject
     * @return
     * @throws XPathExpressionException
     */
    static Collection<AclContext> policyMatcher(Subject subject, Collection<? extends Policy> policyLister)
            throws InvalidCollection {
        ArrayList<AclContext> matchedContexts = new ArrayList<AclContext>();
        int i = 0;
        for (final Policy policy : policyLister) {


            // What constitutes a match?
            // * The username matches exactly 1 in the context.
            // * 1 subject group matches 1 group.  non disjoint sets.
            //
            // First match stops the search, for this particular policy.

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

    @Override
    public String toString() {
        return "PoliciesDocument{" +
               "file=" + file +
               '}';
    }

    public static class Context implements AclContext {
        public Context(Node policy) {
            super();
            this.policy = policy;
        }

        final private Node policy;


        /* (non-Javadoc)
         * @see com.dtolabs.rundeck.core.authorization.providers.AclContext#includes(java.util.Map, java.lang.String)
         */
        public ContextDecision includes(Map<String, String> resource, String action) {
            // keep track of each context and the the resulting grant or rejection
            List<ContextEvaluation> evaluations = new ArrayList<ContextEvaluation>();

            if (null != resource.get("type") && !"job".equals(resource.get("type"))) {

                evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED, "Legacy format: Unsupported resource type "+ resource
                    .get("type")));
                return new ContextDecision(Explanation.Code.REJECTED, false, evaluations);
            }
            try {
                StringBuilder filter = new StringBuilder();
                for (final String next : resource.keySet()) {
                    if("type".equals(next)){
                        continue;
                    }
                    if (filter.length() > 0) {
                        filter.append(" and ");
                    }
                    if (next != null && next.length() > 0) {
                        filter.append('@').append(next);
                    }
                }

                String filterString = filter.toString();
                if(!commandFilterCache.containsKey(filterString)) {
                    commandFilterCache.put(filterString, xpath.compile("descendant-or-self::command["+filterString+"]"));
                }

                NodeList commands = (NodeList) commandFilterCache.get(filterString).evaluate(policy, XPathConstants.NODESET);

                for(int i = 0; i < commands.getLength(); i++) {

                    Node command = commands.item(i);
                    NamedNodeMap attributes = command.getAttributes();

                    // check actions.
                    Node actionAttr = attributes.getNamedItem("actions");
                    if(actionAttr == null) {
                        // assume no actions can be taken.
                        evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_NO_ACTIONS_DECLARED,
                                generateJobName(this.policy, command)));
                        continue;
                    }

                    String actionsNodeValue = actionAttr.getNodeValue();
                    if(actionsNodeValue.length() <= 0) {
                        evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_ACTIONS_DECLARED_EMPTY,
                                generateJobName(this.policy, command)));
                        continue;
                    }

                    // special case.  '*' matches anything.
                    if(!"*".equals(actionsNodeValue)) {
                        List<String> actions = Arrays.asList(actionsNodeValue.split(","));
                        if(!actions.contains(action)) {
                            evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_NO_ACTIONS_MATCHED,
                                    generateJobName(this.policy, command)));
                            continue;
                        }
                    }

                    // all must match
                    boolean matched = true;
                    for (final String key : resource.keySet()) {
                        if ("type".equals(key)) {
                            continue;
                        }
                        Node attr = attributes.getNamedItem(key);

                        // if resource matches command attribute, either literally or via regex on command, continue.
                        String matchField = attr.getNodeValue();

                        // special case.
                        if(matchField.equals("*")) {
                            continue;
                        }

                        String input = resource.get(key);
                        if(input == null || input.length() <= 0) {
                            evaluations.add(
                                    new ContextEvaluation(Explanation.Code.REJECTED_NO_RESOURCE_PROPERTY_PROVIDED,
                                            generateJobName(policy, command)));
                            matched = false;
                            break;
                        }

                        if(!Pattern.matches(matchField, input)) {
                            matched = false;
                            evaluations.add(
                                    new ContextEvaluation(Explanation.Code.REJECTED_RESOURCE_PROPERTY_NOT_MATCHED,
                                            generateJobName(policy, command)));
                            break;
                        }

                    }

                    if(matched) {
                        evaluations.add(new ContextEvaluation(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, generateJobName(policy, command)));
                        return new ContextDecision(Explanation.Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, true, evaluations);
                    }
                }
            } catch (XPathExpressionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return new ContextDecision(Explanation.Code.REJECTED_CONTEXT_EVALUATION_ERROR, false);
            }
            evaluations.add(new ContextEvaluation(Explanation.Code.REJECTED_COMMAND_NOT_MATCHED, generatePolicyName(policy)));
            return new ContextDecision(Explanation.Code.REJECTED_COMMAND_NOT_MATCHED, false, evaluations);
        }

        private String generatePolicyName(Node policy2) {
            StringBuilder sb = new StringBuilder();
            buildNodeString(policy2, sb);
            return sb.toString();
        }

        private String generateJobName(Node policy2, Node command) {
            StringBuilder sb = new StringBuilder();

            Node parent = command.getParentNode();
            List<Node> hierarchy = new ArrayList<Node>();
            while(!"policies".equals(parent.getNodeName())) {
                hierarchy.add(parent);
                parent = parent.getParentNode();
            }
            Collections.reverse(hierarchy);
            for(Node node : hierarchy) {
                buildNodeString(node, sb);
                sb.append(" / ");
            }

            buildNodeString(command, sb);
            return sb.toString();
        }

        /**
         * @param node
         * @param sb
         */
        private void buildNodeString(Node node, StringBuilder sb) {
            sb.append(node.getNodeName());
            sb.append('[');
            NamedNodeMap nodeAttributes = node.getAttributes();
            for(int i = 0; i < nodeAttributes.getLength(); i++) {
                Node item = nodeAttributes.item(i);

                sb.append(item.getNodeName());
                sb.append(':');
                sb.append(item.getNodeValue());

                // add space after if more attributes are comming.
                if(i + 1 < nodeAttributes.getLength()) {
                    sb.append(' ');
                }
            }
            sb.append("] ");
        }

        @Override
        public String toString() {
            return "Context: " + this.generatePolicyName(this.policy);
        }
    }
}
