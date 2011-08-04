/**
 * 
 */
package com.dtolabs.rundeck.core.authorization.providers;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.security.auth.Subject;
import javax.xml.xpath.XPathExpressionException;

import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import com.dtolabs.rundeck.core.authorization.Attribute;
import com.dtolabs.rundeck.core.authorization.Explanation.Code;
import com.dtolabs.rundeck.core.authorization.providers.PoliciesXml.Context;

/**
 * @author noahcampbell
 */
public class PoliciesYaml implements PolicyCollection {

    private final Set<YamlPolicy> all = new HashSet<YamlPolicy>();
    
    
    public PoliciesYaml(File file) throws Exception {
        Yaml yaml = new Yaml();
        for(Object yamlDoc : yaml.loadAll(new FileInputStream(file))) {
            all.add(new YamlPolicy(yamlDoc));
        }
    }

    @Override
    public Collection<String> groupNames() throws InvalidCollection {
        List<String> groups = new ArrayList<String>();
        for(YamlPolicy policy: all) {
            for(Object policyGroup: policy.getGroups()) {
                groups.add(policyGroup.toString());
            }
        }
        return groups;
    }

    @Override
    public long countPolicies() throws InvalidCollection {
        return all.size();
    }

    @Override
    public Collection<AclContext> matchedContexts(Subject subject, Set<Attribute> environment)
            throws InvalidCollection {
        return PoliciesDocument.policyMatcher(subject, all);
        
    }
    
    public static final class YamlPolicyRule {
        public String actions;
        public String resource;
    }
    
    public static final class YamlPolicy implements Policy {

        public Map rawInput;
        
        private Set<String> usernames = new HashSet<String>();
        private Set<Object> groups = new HashSet<Object>();
        private static ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<String, Pattern>();
        
        public YamlPolicy(Object yamlDoc) {
            rawInput = (Map)yamlDoc;
            parseByClause();
        }

        @Override
        public Set<String> getUsernames() {
            return usernames;
        }
        
        @Override
        public Set<Object> getGroups() {
            return groups;
        }
        
        @Override
        public AclContext getContext() {
            return new AclContext() {
                private String description = "Not Evaluated: " + super.toString();
                
                public String toString() {
                    return "Context: " + description;
                }
                
                @SuppressWarnings("rawtypes")
                @Override
                public ContextDecision includes(Map<String, String> resourceMap, String action) {
                    String resource = defineResource(resourceMap);
                    List<ContextEvaluation> evaluations = new ArrayList<ContextEvaluation>();
                    Object descriptionValue = rawInput.get("description");
                    if( descriptionValue == null || !(descriptionValue instanceof String)) {
                        evaluations.add(new ContextEvaluation(Code.REJECTED_NO_DESCRIPTION_PROVIDED, "Policy is missing a description."));
                        return new ContextDecision(Code.REJECTED_NO_DESCRIPTION_PROVIDED, false, evaluations);
                    }
                    
                    description = (String)descriptionValue;
                    
                    Object rulesValue = rawInput.get("rules");
                    if( !(rulesValue instanceof Map) ) {
                        evaluations.add(new ContextEvaluation(Code.REJECTED_NO_RULES_DECLARED, "No rules declared on policy"));
                        return new ContextDecision(Code.REJECTED_NO_RULES_DECLARED, false, evaluations);
                    }
                    Map rules = (Map)rulesValue;
                    
                    Set<Map.Entry> entries = rules.entrySet();
                    for(Map.Entry entry : entries) {
                        Object ruleKey = entry.getKey();
                        if(!(ruleKey instanceof String)) { 
                            evaluations.add(new ContextEvaluation(Code.REJECTED_CONTEXT_EVALUATION_ERROR, "Invalid key type: " + ruleKey.getClass().getName()));
                            continue;
                        }
                        
                        String rule = (String)ruleKey;
                        if(rule == null || rule.length() == 0) {
                            evaluations.add(new ContextEvaluation(Code.REJECTED_CONTEXT_EVALUATION_ERROR, "Resource is empty or null"));
                        }
                        
                        if(!patternCache.containsKey(rule)) {
                            patternCache.putIfAbsent(rule, Pattern.compile(rule));
                        }
                        Pattern pattern = patternCache.get(rule);
                        Matcher matcher = pattern.matcher(resource);
                        if(matcher.matches()) {
                            // TODO include action1,action2
                            Map ruleMap = (Map) entry.getValue();
                            Object actionsKey = ruleMap.get("actions");
                            if(actionsKey == null) {
                                evaluations.add(new ContextEvaluation(Code.REJECTED_ACTIONS_DECLARED_EMPTY, "No actions configured"));
                                continue;
                            }
                            
                            if(actionsKey instanceof String) {
                                String actions = (String) actionsKey;
                                if("*".equals(actions) || actions.contains(action)) {
                                    evaluations.add(new ContextEvaluation(Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, description + ": rule: " + rule + " action: " + actions));
                                    return new ContextDecision(Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, true, evaluations);
                                }
                            } else if(actionsKey instanceof List) {
                                List actions = (List) actionsKey;
                                if(actions.contains(action)) {
                                    evaluations.add(new ContextEvaluation(Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, description + ": rule: " + rule + " action: " + actions));
                                    return new ContextDecision(Code.GRANTED_ACTIONS_AND_COMMANDS_MATCHED, true, evaluations);
                                }
                            } else {
                                evaluations.add(new ContextEvaluation(Code.REJECTED_CONTEXT_EVALUATION_ERROR, "Invalid action type."));
                                
                            }
                            
                            evaluations.add(new ContextEvaluation(Code.REJECTED_NO_ACTIONS_MATCHED, "No actions matched"));
                        }
                    }
                    return new ContextDecision(Code.REJECTED, false, evaluations);
                }
            };
        }
        
        private String defineResource(Map<String, String> resource) {
            return resource.get("group") + "/" + resource.get("job");
        }
        
        /**
         * parse the by: clause.
         */
        private void parseByClause() {
            Object byClause = rawInput.get("by");
            if(byClause == null) return;
            if(! (byClause instanceof Map)) return;
            @SuppressWarnings("rawtypes")
            Map by = (Map)byClause;
            @SuppressWarnings("rawtypes")
            Set<Map.Entry> entries = by.entrySet();
            for(@SuppressWarnings("rawtypes") Map.Entry policyGroup : entries) {
                
                if("username".equals(policyGroup.getKey())) {
                    usernames.add(policyGroup.getValue().toString());
                }
                
                if("group".equals(policyGroup.getKey())) {
                    groups.add(policyGroup.getValue());    
                }

                // TODO Support LDAP
            }
        }
        
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer("YamlPolicy[id:");
            sb.append(rawInput.get("id")).append(", groups:");
            for (Object group : getGroups()) {
                sb.append(group.toString()).append(" ");
            }
            sb.append("]");
            return sb.toString();
        }
    }
}
