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
* Policy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: Nov 16, 2010 12:00:32 PM
* 
*/
package com.dtolabs.rundeck.core.authorization.providers;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.DOMException;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.naming.ldap.LdapName;
import javax.naming.InvalidNameException;
import java.util.HashSet;
import java.util.Set;

/**
 * PolicyNode provides the Policy interface on top of a DOM Node
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
public class PolicyNode implements Policy {
    Node policyNode;
    HashSet<String> usernames;
    Set<Object> groups;

    public PolicyNode(Node policyNode) throws XPathExpressionException {
        this.policyNode = policyNode;
        init();
    }

    public Set<String> getUsernames() {
        return usernames;
    }

    private void init() throws XPathExpressionException {
        initUsernames();
        initGroups();
    }

    private void initGroups() throws XPathExpressionException {
        NodeList groupNodes = (NodeList) PoliciesDocument.policyByGroup.evaluate(policyNode, XPathConstants.NODESET);
        groups = new HashSet<Object>(groupNodes.getLength());
        for (int g = 0 ; g < groupNodes.getLength() ; g++) {
            Node group = groupNodes.item(g);
            String ns = group.getNamespaceURI();
            if (ns == null) {
                groups.add(group.getNodeValue());
            } else if (Policies.NS_LDAP.equalsIgnoreCase(ns) || Policies.NS_AD.equalsIgnoreCase(ns)) {
                try {
                    groups.add(new LdapName(group.getNodeValue()));
                } catch (InvalidNameException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (DOMException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }

    }

    private void initUsernames() throws XPathExpressionException {

        NodeList usernameNodes = (NodeList) PoliciesDocument.policyByUserName.evaluate(policyNode,
            XPathConstants.NODESET);

        usernames = new HashSet<String>(usernameNodes.getLength());
        for (int u = 0 ; u < usernameNodes.getLength() ; u++) {
            Node username = usernameNodes.item(u);
            usernames.add(username.getNodeValue());
        }
    }

    public Policies.Context getContext() {
        return new Policies.Context(policyNode);
    }

    public Set<Object> getGroups() {
        return groups;
    }
}
