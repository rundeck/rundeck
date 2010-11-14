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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.LdapGroup;
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Explanation;
import com.dtolabs.rundeck.core.authorization.Explanation.Code;

/**
 * Policies represent the policies as described in the policies file(s).
 * 
 * @author noahcampbell
 */
public class Policies {
    
    private static final String NS_AD = "http://dtolabs.com/rundeck/activedirectory";
    private static final String NS_LDAP = "http://dtolabs.com/rundeck/ldap";
    private final XPath xpath = XPathFactory.newInstance().newXPath();
    private final List<File> policyFiles = new ArrayList<File>();
    private final List<Document> aclpolicies = new ArrayList<Document>();
    
    public Policies() {
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
    }
    
    public void add(File file) throws SAXException, IOException, ParserConfigurationException {
        
        /* Just checking to make sure it's a well formed document. */
        DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
        domFactory.setNamespaceAware(true);
        DocumentBuilder builder = domFactory.newDocumentBuilder();
        aclpolicies.add(builder.parse(file));
        
        this.policyFiles.add(file);
    }
    
    public void remove(File file) {
        this.policyFiles.remove(file);
    }
    
    public int count() {
        int count = 0;
        for(Document f : aclpolicies) {
            try {
                Double n = (Double)xpath.evaluate("count(//policy)", f, XPathConstants.NUMBER);
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
        
        Policies p = new Policies();
        
        for(File f : rootPath.listFiles(new FilenameFilter(){
            public boolean accept(File dir, String name) {
                return name.endsWith(".aclpolicy");
            }})) {
            try {
                p.add(f);
            } catch (SAXException e) {
                throw new PoliciesParseException(e);
            } catch (ParserConfigurationException e) {
                throw new PoliciesParseException(e);
            }
        }            
        
        return p;
    }

    public List<Context> narrowContext(Subject subject, Set<Attribute> environment) {
        
        List<Context> matchedContexts = new ArrayList<Context>();
        for(Document f : aclpolicies) {
            try {
                NodeList policiesToEvaluate = (NodeList)xpath.evaluate("//policy", f, XPathConstants.NODESET);
                for(int i = 0; i < policiesToEvaluate.getLength(); i++) {

                    Node policy = policiesToEvaluate.item(i);                   
                    
                    // What constitutes a match?
                    // * The username matches exactly 1 in the context.
                    // * 1 subject group matches 1 group.  non disjoint sets.
                    //
                    // First match stops the search.
                    
                    // TODO: time of day check.
                    
                    
                    NodeList usernames = (NodeList) xpath.evaluate("by/user/@username", policy, XPathConstants.NODESET);
                    
                    Set<String> policyUsers = new HashSet<String>(usernames.getLength());
                    for(int u = 0; u < usernames.getLength(); u++) {
                        Node username = usernames.item(u);
                        policyUsers.add(username.getNodeValue());
                    }
                    
                    Set<Username> userPrincipals = subject.getPrincipals(Username.class);
                    if(userPrincipals.size() > 0) {
                        Set<String> usernamePrincipals = new HashSet<String>();
                        for(Username username: userPrincipals) {
                            usernamePrincipals.add(username.getName());
                        }
                        
                        if(!Collections.disjoint(policyUsers, usernamePrincipals)) {
                            matchedContexts.add(new Context(policy));
                            break;
                        }
                    }
                    
                    Set<Group> groupPrincipals = subject.getPrincipals(Group.class);
                    if(groupPrincipals.size() > 0) {
                        // no username matched, check groups.
                        NodeList groups = (NodeList) xpath.evaluate("by/group/@name | by/group/@ldap:name", policy, XPathConstants.NODESET);
                        Set<Object> policyGroups = new HashSet<Object>(groups.getLength());
                        for(int g = 0; g < groups.getLength(); g++) {
                            Node group = groups.item(g);
                            String ns = group.getNamespaceURI();
                            if(ns == null) {
                                policyGroups.add(group.getNodeValue());
                            } else if (NS_LDAP.equalsIgnoreCase(ns) || NS_AD.equalsIgnoreCase(ns)) {
                                try {
                                    policyGroups.add(new LdapName(group.getNodeValue()));
                                } catch (InvalidNameException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                } catch (DOMException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }
                        }
                        Set<Object> groupNames = new HashSet<Object>();
                        for(Group groupPrincipal: groupPrincipals) {
                            if(groupPrincipal instanceof LdapGroup) {
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
                        if(!Collections.disjoint(policyGroups, groupNames)) {
                            matchedContexts.add(new Context(policy));
                            continue;
                        }
                    }
                    
//                    if(subject.getPrincipals(LdapGroupPrincipal.class).size() > 0) {
//                      //todo check against ldap.    
//                    }
                }
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

    public class Context {
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
                NodeList commands = (NodeList) xpath.evaluate("descendant-or-self::command["+filter.toString()+"]", policy, XPathConstants.NODESET);
                
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
        for(Document f: aclpolicies) {
            try {
                
                NodeList groups = (NodeList) xpath.evaluate("//by/group/@ldap:name | //by/group/@name", 
                        f, XPathConstants.NODESET);
                
                for(int i = 0; i < groups.getLength(); i++) {
                    
                    String result = groups.item(i).getNodeValue();
                    if(result == null || result.length() <= 0) continue;
                    results.add(result);    
                }
            } catch (XPathExpressionException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } 
        }
        
        return results;
    }
}
