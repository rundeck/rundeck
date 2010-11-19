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
import com.dtolabs.rundeck.core.authorization.Explanation;
import com.dtolabs.rundeck.core.authorization.Explanation.Code;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.security.auth.Subject;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Policies represent the policies as described in the policies file(s).
 * 
 * @author noahcampbell
 */
public class Policies {
    
    static final String NS_AD = "http://dtolabs.com/rundeck/activedirectory";
    static final String NS_LDAP = "http://dtolabs.com/rundeck/ldap";
    private static final XPath xpath = XPathFactory.newInstance().newXPath();
    private final List<File> policyFiles = new ArrayList<File>();

    private final XPathExpression count;
    private final XPathExpression allPolicies;
    private final XPathExpression byUserName;
    private final XPathExpression byGroup;

    private PoliciesCache cache;

    
    public Policies(final PoliciesCache cache) {
        this.cache = cache;
        xpath.setNamespaceContext(new NamespaceContext() {

            @SuppressWarnings("rawtypes")
            public Iterator getPrefixes(String namespaceURI) { return null; }
            public String getPrefix(String namespaceURI) { return null; }

            public String getNamespaceURI(String prefix) {
                if(prefix.equals("ldap")) {
                    return NS_LDAP;
                } else if(prefix.equals("ActiveDirectory")) {
                    return NS_AD;
                } else {
                    return ""; // 1.6 = XMLConstants.NULL_NS_URI;
                }
            }
        });

        try {
            this.count = xpath.compile("count(//policy)");
            this.allPolicies = xpath.compile("//policy");
            this.byUserName = xpath.compile("by/user/@username");
            this.byGroup = xpath.compile("by/group/@name | by/group/@ldap:name");
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    public int count() {
        int count = 0;
        for(PoliciesDocument f : cache) {

            try {
                Double n = f.countPolicies();
                count += n;
            } catch (XPathExpressionException e) {
                // TODO squash
            }
        }
        return count;
    }
    
    /**
     * Load the policies contained in the root path.
     * @param rootPath
     * @return
     * @throws PoliciesParseException Thrown when there is a problem parsing a file.
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

    public List<Context> narrowContext(Subject subject, Set<Attribute> environment) {
        
        List<Context> matchedContexts = new ArrayList<Context>();
        for(final PoliciesDocument f : cache) {
            try {
                matchedContexts.addAll(f.matchedContexts(subject, environment));
            } catch (XPathExpressionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }
        return matchedContexts;
    }
    
    public static class ContextEvaluation {
        private ContextEvaluation(Code id, String command) {
            this.id = id;
            this.command = command;
        }
        public final Code id;
        public final String command;
        
        @Override
        public String toString() {
            return command + " => " + id;
        }
    }
    
    public static class ContextDecision implements Explanation {
        
        private final Code id;
        private final boolean granted;
        private final List<ContextEvaluation> evaluations;
        
        public ContextDecision(Code id, boolean granted, List<ContextEvaluation> evaluations) {
            this.id = id;
            this.granted = granted;
            this.evaluations = evaluations;
        }
        
        public ContextDecision(Code id, boolean granted) {
            this(id, granted, new ArrayList<ContextEvaluation>());
        }
        
        public boolean granted() {
            return this.granted;
        }
        
        public Code getCode() {
            return this.id;
        }

        public void describe(PrintStream out) {
            for(ContextEvaluation ce : this.evaluations) {
                out.println("\t" + ce);
            }
        }
        
    }
    
    final static private Map<String, XPathExpression> commandFilterCache = new HashMap<String, XPathExpression>();
    
    public static class Context {
        public Context(Node policy) {
            super();
            this.policy = policy;
        }

        final private Node policy;
        

        public ContextDecision includes(Map<String, String> resource, String action) {
            // keep track of each context and the the resulting grant or rejection
            List<ContextEvaluation> evaluations = new ArrayList<ContextEvaluation>();
            
            try {
                StringBuilder filter = new StringBuilder();
                Iterator<String> iter = resource.keySet().iterator();
                while(iter.hasNext()) {
                    
                    String next = iter.next();
                    
                    if(next != null && next.length() > 0) {
                        filter.append('@').append(next);
                    }
                    
                    if(iter.hasNext()) {
                        filter.append(" and ");
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
                        evaluations.add(new ContextEvaluation(Code.REJECTED_NO_ACTIONS_DECLARED, 
                                generateJobName(this.policy, command)));
                        continue;
                    }
                    
                    String actionsNodeValue = actionAttr.getNodeValue();
                    if(actionsNodeValue.length() <= 0) {
                        evaluations.add(new ContextEvaluation(Code.REJECTED_ACTIONS_DECLARED_EMPTY, 
                                generateJobName(this.policy, command)));
                        continue;
                    }
                    
                    // special case.  '*' matches anything.
                    if(!"*".equals(actionsNodeValue)) {
                        List<String> actions = Arrays.asList(actionsNodeValue.split(","));
                        if(!actions.contains(action)) {
                            evaluations.add(new ContextEvaluation(Code.REJECTED_NO_ACTIONS_MATCHED, 
                                    generateJobName(this.policy, command)));
                            continue;
                        }
                    }
                    
                    // all must match
                    boolean matched = true;
                    Iterator<String> resourceKeyIter = resource.keySet().iterator();
                    while(matched && resourceKeyIter.hasNext()) {
                        String key = resourceKeyIter.next();
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
                                    new ContextEvaluation(Code.REJECTED_NO_RESOURCE_PROPERTY_PROVIDED, 
                                            generateJobName(policy, command)));
                            matched = false;
                            break;
                        }
                        
                        if(!Pattern.matches(matchField, input)) {
                            matched = false;
                            evaluations.add(
                                    new ContextEvaluation(Code.REJECTED_RESOURCE_PROPERTY_NOT_MATCHED, 
                                            generateJobName(policy, command)));
                            break;
                        }
                               
                    }
                    
                    if(matched) {
                        evaluations.add(new ContextEvaluation(Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, generateJobName(policy, command)));
                        return new ContextDecision(Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, true, evaluations);
                    }
                }
            } catch (XPathExpressionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return new ContextDecision(Code.REJECTED_CONTEXT_EVALUATION_ERROR, false);
            }
            evaluations.add(new ContextEvaluation(Code.REJECTED_COMMAND_NOT_MATCHED, generatePolicyName(policy)));
            return new ContextDecision(Code.REJECTED_COMMAND_NOT_MATCHED, false, evaluations);
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
            while(parent.getNodeName() != "policies") {
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
            return "Context";
        }
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getName());
        builder.append(" [");
        Iterator<File> iter = this.policyFiles.iterator();
        while(iter.hasNext()) {
            builder.append(iter.next());
            if(iter.hasNext()) {
                builder.append(", ");
            }
        }
        builder.append("]");
        
        return  builder.toString();
    }

    /**
     * @return
     */
    @Deprecated
    public List<String> listAllRoles() {
        List<String> results = new ArrayList<String>();
        for(PoliciesDocument f: cache) {
            try {
                results.addAll(f.groupNames());
                
            } catch (XPathExpressionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }

        return results;
    }
}
