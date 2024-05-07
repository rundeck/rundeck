/*
 * Copyright 2018 Rundeck, Inc. (http://rundeck.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.authorization.providers.yaml.model;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;
import org.yaml.snakeyaml.nodes.*;

import java.util.*;

/**
 * @author greg
 * @since 3/20/17
 */
public class YamlPolicyDocConstructor extends Constructor {
    public YamlPolicyDocConstructor() {
        super(ACLPolicyDoc.class, new LoaderOptions());
        this.yamlConstructors.put(null, undefinedConstructor);
        yamlConstructors.put(new Tag(ACLPolicyDoc.class), new ACLPolicyDocConstructYamlObject());
        yamlClassConstructors.put(NodeId.mapping, new YamlPolicyDocConstruct());
        yamlClassConstructors.put(NodeId.scalar, new ACLPolicyDocEmptyConstructScalar());
    }

    /**
     * return null if root node is blank scalar, e.g. doc separator in yaml with empty content
     */
    class ACLPolicyDocEmptyConstructScalar
            extends Constructor.ConstructScalar {
        @Override
        public Object construct(final Node nnode) {
            if(nnode.getType()==ACLPolicyDoc.class){
                if (nnode instanceof ScalarNode) {
                    ScalarNode scalarNode = (ScalarNode) nnode;
                    if ("".equals(scalarNode.getValue())) {
                        return null;
                    }
                }
            }
            return super.construct(nnode);
        }
    }

    static Set<String> ALLOWED_POLICY_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "by", "notBy","id", "for", "context", "description"
    )));
    static Set<String> ALLOWED_CONTEXT_KEYS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "project","application"
    )));

    /**
     * allow AclPolicyDoc type to be constructed
     */
    class ACLPolicyDocConstructYamlObject
            extends Constructor.ConstructYamlObject {
        //subtype required due to protected class
    }

    /**
     * validate document structure to match expectations and fail with readable messages
     */
    class YamlPolicyDocConstruct extends Constructor.ConstructMapping {

        @Override
        protected Object constructJavaBean2ndStep(MappingNode node, Object object) {
            Class<?> type = node.getType();
            if (type.equals(ACLPolicyDoc.Context.class)) {
                for (NodeTuple tuple : node.getValue()) {
                    ScalarNode keyNode;
                    if (tuple.getKeyNode() instanceof ScalarNode) {
                        // key must be scalar
                        keyNode = (ScalarNode) tuple.getKeyNode();
                    } else {
                        throw new YAMLException("Keys must be scalars but found: " + tuple.getKeyNode());
                    }
                    // keys can only be Strings
                    keyNode.setType(String.class);
                    String key = (String) constructObject(keyNode);
                    if (!ALLOWED_CONTEXT_KEYS.contains(key)) {
                        throw new YAMLException(
                                "Context section should contain only 'application:' or 'project:'");
                    }
                }
            } else {
                if (type.equals(ACLPolicyDoc.class)) {
                    //declare types for the `for` section
                    for (NodeTuple tuple : node.getValue()) {
                        ScalarNode keyNode;
                        if (tuple.getKeyNode() instanceof ScalarNode) {
                            // key must be scalar
                            keyNode = (ScalarNode) tuple.getKeyNode();
                        } else {
                            throw new YAMLException("Keys must be scalars but found: " + tuple.getKeyNode());
                        }
                        Node valueNode = tuple.getValueNode();
                        // keys can only be Strings
                        keyNode.setType(String.class);
                        String key = (String) constructObject(keyNode);
                        if (!ALLOWED_POLICY_KEYS.contains(key)) {
                            throw new YAMLException(
                                    String.format(
                                            "Policy contains invalid keys: [%s], allowed keys: [by, id, for, context," +
                                            " " +
                                            "description]",
                                            key
                                    ));
                        }
                        if ("for".equals(key)) {
                            if (valueNode instanceof MappingNode) {
                                MappingNode forValue = (MappingNode) valueNode;
                                if (forValue.getValue().size() < 1) {
                                    throw new YAMLException("Section 'for:' cannot be empty");
                                }
                                for (NodeTuple nodeTuple : forValue.getValue()) {
                                    Node listNode = nodeTuple.getValueNode();
                                    ScalarNode forkeyNode;
                                    if (nodeTuple.getKeyNode() instanceof ScalarNode) {
                                        // key must be scalar
                                        forkeyNode = (ScalarNode) nodeTuple.getKeyNode();
                                    } else {
                                        throw new YAMLException("Keys must be scalars but found: " +
                                                                nodeTuple.getKeyNode());
                                    }
                                    // keys can only be Strings
                                    forkeyNode.setType(String.class);
                                    String forkey = (String) constructObject(forkeyNode);
                                    if (listNode instanceof SequenceNode) {
                                        SequenceNode seq = (SequenceNode) listNode;
                                        seq.setType(List.class);
                                        seq.setListType(ACLPolicyDoc.TypeRule.class);
                                        List<Node> value = seq.getValue();
                                        for (int x = 0; x < value.size(); x++) {
                                            Node node1 = value.get(x);
                                            if (!(node1 instanceof MappingNode)) {

                                                throw new YAMLException(String.format(
                                                        "Type rule 'for: { %s: [...] }' entry at index [%d] expected " +
                                                        "a " +
                                                        "Map but saw: %s",
                                                        forkey,
                                                        x + 1,
                                                        node1.getNodeId()
                                                ));
                                            }
                                        }
                                    } else {
                                        throw new YAMLException("Expected 'for: { " +
                                                                forkey +
                                                                ": <...> }' to be a Sequence, but was [" +
                                                                listNode.getNodeId() +
                                                                "]");
                                    }
                                }
                            } else {
                                throw new YAMLException(String.format(
                                        "Expected 'for:' section to contain a map, but was %s",
                                        valueNode.getNodeId()
                                ));
                            }
                        }
                    }
                }
            }
            return super.constructJavaBean2ndStep(node, object);
        }
    }
}
