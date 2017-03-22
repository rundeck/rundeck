/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
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

/*
* YamlPolicy.java
* 
* User: Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
* Created: 8/25/11 11:25 AM
* 
*/
package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.*;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.core.utils.PairImpl;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.log4j.Logger;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * YamlPolicy implements a policy from a yaml document input or map.
 *
 * @author Greg Schueler <a href="mailto:greg@dtosolutions.com">greg@dtosolutions.com</a>
 */
final class YamlPolicy implements Policy,AclRuleSetSource {
    final static Logger logger = Logger.getLogger(YamlPolicy.class.getName());
    public static final String TYPE_PROPERTY = "type";
    public static final String FOR_SECTION = "for";
    public static final String JOB_TYPE = "job";
    public static final String ACTIONS_SECTION = "actions";
    public static final String CONTEXT_SECTION = "context";
    public static final String BY_SECTION = "by";
    public static final String ID_SECTION = "id";
    public static final String USERNAME_KEY = "username";
    public static final String GROUP_KEY = "group";
    private static final String DESCRIPTION_KEY = "description";
    public static final String PROJECT_CONTEXT = "project";
    public static final String APPLICATION_CONTEXT = "application";
    public Map policyInput;

    private Set<String> usernames = new HashSet<String>();
    private Set<String> groups = new HashSet<String>();
    YamlRuleSetConstructor constructor;
    private YamlEnvironmentalContext environment;
    private Set<AclRule> rules = new HashSet<>();

    private String sourceIdent;
    private int sourceIndex;
    private ValidationSet validation;

    private YamlPolicy(final Set<Attribute> context,final Map policyInput, final String sourceIdent, final int sourceIndex,ValidationSet validation) {
        this.policyInput = policyInput;
        this.sourceIdent = sourceIdent;
        this.sourceIndex = sourceIndex;
        this.validation = validation;
        parseByClause();
        constructor = new YamlRuleSetConstructor(
                this.policyInput,
                this.validation,
                new TypeRuleSetConstructorFactory() {
                    @Override
                    public RuleSetConstructor createRuleSetConstructor(final String type, final List typeSection) {
                        return new RuleSetConstructor() {
                            @Override
                            public Set<AclRule> createRules(final AclRuleBuilder builder) {
                                return YamlPolicy.this.createRules(type, typeSection, builder);
                            }
                        };
                    }
                }
        );
        parseEnvironment(context);
        validate();
        enumerateRules();
    }

    private List<String> allowed = Arrays.asList(
            BY_SECTION,
            ID_SECTION,
            FOR_SECTION,
            CONTEXT_SECTION,
            DESCRIPTION_KEY
    );
    private List<String> allowedContexts = Arrays.asList(
            PROJECT_CONTEXT,
            APPLICATION_CONTEXT
    );
    @SuppressWarnings("unchecked")
    private void validate() {
        HashSet disallowed = new HashSet<>(policyInput.keySet());
        disallowed.removeAll(allowed);
        if (disallowed.size() != 0) {
            throw new AclPolicySyntaxException(
                    "Policy contains invalid keys: " +
                    disallowed +
                    ", allowed keys: " +
                    allowed
            );
        }
    }

    static YamlPolicyCollection.YamlSourceLoader<Map> loader(YamlSource source, ValidationSet validation) {
        return new YamlPolicyCollection.YamlSourceLoader<Map>() {
            @Override
            public Iterable<Map> loadAll() throws IOException {
                final Yaml yaml = new Yaml(new Constructor(Map.class));
                Iterable<Object> objects = source.loadAll(yaml);
                Iterator<Object> iterator = objects.iterator();
                return () -> new Iterator<Map>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Map next() {
                        Object next = iterator.next();
                        if (!(next instanceof Map)) {
                            if(validation!=null) {
                                validation.addError(
                                        source.getIdentity(),
                                        "Expected a Map document, but was type: " + next.getClass()
                                );
                            }
                            return null;
                        }
                        return (Map) next;
                    }
                };
            }

            ;

            @Override
            public void close() throws IOException {
                source.close();
            }
        };
    }

    static YamlPolicyCollection.YamlPolicyCreator<Map> creator(
            final Set<Attribute> forcedContext,
            final ValidationSet validation
    )
    {
        return new YamlPolicyCollection.YamlPolicyCreator<Map>() {
            @Override
            public Policy createYamlPolicy(final Map policyInput, final String sourceIdent, final int sourceIndex)
                    throws AclPolicySyntaxException
            {
                return YamlPolicy.createYamlPolicy(
                        forcedContext,
                        policyInput,
                        sourceIdent,
                        sourceIndex,
                        validation
                );
            }
        };
    }

    static YamlPolicy createYamlPolicy(final Map policyInput, final String sourceIdent, final int sourceIndex,ValidationSet validation) {
        return new YamlPolicy(null, policyInput, sourceIdent, sourceIndex,validation);
    }

    static YamlPolicy createYamlPolicy(
            final Set<Attribute> context,
            final Map policyInput,
            final String sourceIdent,
            final int sourceIndex,
            ValidationSet validation
    )
    {
        return new YamlPolicy(context, policyInput, sourceIdent, sourceIndex,validation);
    }

    /**
     * generate list of AclRules
     */
    private void enumerateRules() {
        if(null==environment){
            return;
        }
        String description = policyInput.containsKey(DESCRIPTION_KEY)?policyInput.get(DESCRIPTION_KEY).toString():null;
        AclRuleBuilder envProto = AclRuleBuilder.builder().environment(
                environment.toBasic()
        ).description(description).sourceIdentity(sourceIdent);

        for (String username : usernames) {
            AclRuleBuilder ruleBuilder = AclRuleBuilder.builder(envProto).username(username);
            rules.addAll(constructor.createRules(ruleBuilder));
        }
        for (String group : groups) {
            AclRuleBuilder ruleBuilder = AclRuleBuilder.builder(envProto).group(group);
            rules.addAll(constructor.createRules(ruleBuilder));
        }
    }

    YamlPolicy(final Map policyInput, final File sourceFile, final int sourceIndex,ValidationSet validation) {
        this(null, policyInput, sourceFile.getAbsolutePath(), sourceIndex,validation);
    }

    String identify() {
        return null != policyInput.get("id") ? policyInput.get("id").toString()
                : (null != sourceIdent ? (sourceIdent) : "(unknown source)")
                ;
    }

    @Override
    public AclRuleSet getRuleSet() {
        return new AclRuleSetImpl(rules);
    }

    public Set<String> getUsernames() {
        return usernames;
    }

    private boolean envchecked;

    public EnvironmentalContext getEnvironment() {
        return environment;
    }

    private void parseEnvironment(Set<Attribute> forcedContext) {
        //create
        final Object ctxClause = policyInput.get(CONTEXT_SECTION);
        if (null != forcedContext) {
            if(null != ctxClause) {
                throw new AclPolicySyntaxException(
                        "Context section should not be specified, it is already set to: " +
                        AuthorizationUtil.contextAsString(forcedContext)
                );
            }
            environment = new YamlEnvironmentalContext(EnvironmentalContext.URI_BASE, forcedContext);
        }else {
            if (null == ctxClause || !(ctxClause instanceof Map)) {
                throw new AclPolicySyntaxException(
                        null == ctxClause
                        ? "Required 'context:' section was not present"
                        : "Context section is not valid: expected a Map, but it was: " + ctxClause.getClass().getName()
                );
            }
            Map ctxClause1 = (Map) ctxClause;
            environment = new YamlEnvironmentalContext(EnvironmentalContext.URI_BASE, ctxClause1);
            if (!environment.isValid()) {
                throw new AclPolicySyntaxException(
                        "Context section is not valid: " +
                        ctxClause +
                        environment.getValidation()
                );
            }
            if (ctxClause1.size() != 1) {
                throw new AclPolicySyntaxException(
                        "Context section should have only one entry: 'application:' or 'project:'"
                );
            }
            if (!allowedContexts.containsAll(ctxClause1.keySet())) {
                throw new AclPolicySyntaxException(
                        "Context section should contain only 'application:' or 'project:'"
                );
            }
        }

    }


    static class YamlEnvironmentalContext implements EnvironmentalContext {
        Map<URI, String> matcher = new HashMap<URI, String>();
        Map<URI, Pattern> matcherRegex = new HashMap<URI, Pattern>();
        private boolean valid = false;
        private String validation;
        private String description;
        static private Comparator<Attribute> comparator = new Comparator<Attribute>() {
            public int compare(Attribute attribute, Attribute attribute2) {
                int u = attribute.property.compareTo(attribute2.property);
                if (u == 0) {
                    return attribute.value.compareTo(attribute2.value);
                } else {
                    return u;
                }
            }
        };
        EnvironmentalContext toBasic() {
            if (matcherRegex.size() != 1 && matcher.size() != 1) {
                throw new IllegalStateException("Expected environmental context to contain only one entry");
            }
            if (matcherRegex.size() == 1) {
                URI key;
                Pattern value;
                Map.Entry<URI, Pattern> next = matcherRegex.entrySet().iterator().next();
                key = next.getKey();
                value = next.getValue();
                return BasicEnvironmentalContext.patternContextFor(
                        key.toString().substring(EnvironmentalContext.URI_BASE.length()),
                        value.toString()
                );
            }else {
                Map.Entry<URI, String> next = matcher.entrySet().iterator().next();
                URI key=next.getKey();
                String value=next.getValue();
                return BasicEnvironmentalContext.staticContextFor(
                        key.toString().substring(
                                EnvironmentalContext.URI_BASE.length()
                        ),
                        value
                );
            }

        }

        /**
         * Create context from attribute set
         * @param uriPrefix prefix
         * @param ctx context
         */
        YamlEnvironmentalContext(final String uriPrefix, final Set<Attribute> ctx) {
            for (Attribute attribute : ctx) {
                if(attribute.getProperty().toString().startsWith(uriPrefix)) {
                    URI key = attribute.getProperty();
                    String value = attribute.getValue();
                    matcher.put(key, value);
                    try {
                        Pattern compile = Pattern.compile(value);
                        matcherRegex.put(key, compile);
                    } catch (PatternSyntaxException e) {
                    }
                }
            }
            valid =  matcher.size() >= 1;

            description = "YamlEnvironmentalContext{" +
                          (valid ?
                           ", valid=" + valid +
                           ", context='" + matcher + '\'' +
                           '}'
                                 :
                           ", valid=" + valid +
                           ", validation='" + getValidation() + '\'' +
                           '}');
        }
        YamlEnvironmentalContext(final String uriPrefix, final Map ctx) {
            boolean invalidentry = false;
            ArrayList<String> errors = new ArrayList<String>();
            for (final Object o : ctx.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                if (entry.getKey() instanceof String) {
                    String path = (String) entry.getKey();
                    final URI uri;
                    try {
                        uri = new URI(uriPrefix + path);
                    } catch (URISyntaxException e) {
                        errors.add("Context section: " + entry.getKey() + ": invalid URI: " + e.getMessage());
                        invalidentry = true;
                        continue;
                    }
                    if (entry.getValue() instanceof String) {
                        String value = (String) entry.getValue();
                        matcher.put(uri, value);
                        try {
                            Pattern compile = Pattern.compile(value);
                            matcherRegex.put(uri, compile);
                        } catch (PatternSyntaxException e) {
                        }
                    } else {
                        errors.add(
                                "Context section: " + entry.getKey() + ": expected 'String', saw: " + entry.getValue()
                                        .getClass().getName());
                        invalidentry = true;
                    }
                } else {
                    errors.add("Context section key expected 'String', saw: " + entry.getKey().getClass().getName());
                    invalidentry = true;
                }
            }
            if (errors.size() > 0) {
                final StringBuffer sb = new StringBuffer();
                for (final String error : errors) {
                    if (sb.length() > 0) {
                        sb.append("; ");
                    }
                    sb.append(error);
                }
                validation = sb.toString();
            }
            valid = !invalidentry && matcher.size() >= 1;

            description = "YamlEnvironmentalContext{" +
                    (valid ?
                            ", valid=" + valid +
                                    ", context='" + matcher + '\'' +
                                    '}'
                            :
                            ", valid=" + valid +
                                    ", validation='" + getValidation() + '\'' +
                                    '}');
        }

        public boolean matches(final Set<Attribute> environment) {
            return memo(environment);
        }

        private HashMap<String, Boolean> memoize = new HashMap<String, Boolean>();

        private boolean memo(Set<Attribute> environment) {
            String ident = ident(environment);
            Boolean found = memoize.get(ident);
            if (null == found) {
                found = evaluateMatches(environment);
                memoize.put(ident, found);
            }
            return found;
        }

        private String ident(Set<Attribute> environment) {
            StringBuilder sb = new StringBuilder();
            TreeSet<Attribute> attributes = new TreeSet<Attribute>(comparator);
            attributes.addAll(environment);
            for (Attribute attribute : attributes) {
                sb.append(attribute.hashCode());
                sb.append("/");
            }
            return sb.toString();
        }

        private boolean evaluateMatches(Set<Attribute> environment) {
            //return true if all declared environmental context attributes match in the input
            Set<URI> matchedSet = new HashSet<URI>();
            for (final Attribute attribute : environment) {
                final Pattern pattern = matcherRegex.get(attribute.property);
                final String matchValue = matcher.get(attribute.property);
                if (null != matchValue && matchValue.equals(attribute.value)) {
                    matchedSet.add(attribute.property);
                } else if (null != pattern && pattern.matcher(attribute.value).matches()) {
                    matchedSet.add(attribute.property);
                }
            }
            return valid && matchedSet.size() == matcher.keySet().size();
        }

        public boolean isValid() {
            return valid;
        }

        @Override
        public String toString() {
            return description;
        }

        public String getValidation() {
            return validation;
        }
    }

    public Set<String> getGroups() {
        return groups;
    }

    public RuleSetConstructor getContext() {

        return constructor;
    }

    static interface TypeRuleSetConstructorFactory {
        public RuleSetConstructor createRuleSetConstructor(final String type, final List typeSection);
    }

    public Set<AclRule> createRules(String type, final List typeSection, final AclRuleBuilder prototype) {
        HashSet<AclRule> aclRules = new HashSet<>();
        for (RuleConstructor ruleConstructor : createTypeRules(type, typeSection)) {
            AclRuleBuilder builder = AclRuleBuilder.builder(
                    prototype
            );
            aclRules.add(ruleConstructor.createRule(builder));
        }
        return aclRules;
    }

    List<RuleConstructor> createTypeRules(String type, final List typeSection) {
        final ArrayList<RuleConstructor> rules = new ArrayList<>();
        int i = 1;
        for (final Object o : typeSection) {
            if(!(o instanceof Map)) {
                throw new AclPolicySyntaxException(
                        "Type rule 'for: { " +
                        type +
                        ": [...] }'' entry at index [" + (i) + "] expected a Map but saw: " +
                        o.getClass().getName()
                );
            }
            final Map<String, ?> section = (Map<String, ?>) o;
            rules.add(new TypeRuleConstructor(type, section, validation, i, this));
            i++;
        }
        return rules;
    }


    /**
     * parse the by: clause, allow single string or list of strings for username and grop values
     */
    private void parseByClause() {
        final Object byClause = policyInput.get(BY_SECTION);
        if (byClause == null) {
            throw new AclPolicySyntaxException(
                    "Required '"+BY_SECTION+":' section was not present"
            );
        }
        if (!(byClause instanceof Map)) {
            throw new AclPolicySyntaxException(
                    "Section '" + BY_SECTION + ":' should be a Map, but it was: " + byClause.getClass().getName()
            );
        }
        final Map by = (Map) byClause;

        final Object u = by.get(USERNAME_KEY);
        final Object g = by.get(GROUP_KEY);

        if (null != u) {
            if (u instanceof String) {
                addUsername((String) u);
            } else if (u instanceof Collection) {
                for (final Object o : (Collection) u) {
                    if (o instanceof String) {
                        addUsername((String) o);
                    }else{
                        throw new AclPolicySyntaxException(
                                "Section '" + USERNAME_KEY + ":' should contain only Strings, but saw a: " + o.getClass().getName()
                        );
                    }
                }
            }else{
                throw new AclPolicySyntaxException(
                        "Section '" + USERNAME_KEY + ":' should be a list or a String, but it was: " + u.getClass().getName()
                );
            }
        }

        if (null != g) {
            if (g instanceof String) {
                addGroup((String) g);
            } else if (g instanceof Collection) {
                for (final Object o : (Collection) g) {
                    if (o instanceof String) {
                        addGroup((String) o);
                    }else{
                        throw new AclPolicySyntaxException(
                                "Section '" + GROUP_KEY + ":' should contain only Strings, but saw a: " + o.getClass().getName()
                        );
                    }
                }
            }else{
                throw new AclPolicySyntaxException(
                        "Section '" + GROUP_KEY + ":' should be a list or a String, but it was: " + g.getClass().getName()
                );
            }
        }
        if (groups.size() < 1 && usernames.size() < 1) {
            if(null!=validation) {
                validation.addError(sourceIdent,
                                    "Section '"+BY_SECTION +
                                    ":' is not valid: " +
                                    by +
                                    " it must contain '" +
                                    GROUP_KEY +
                                    ":' and/or '" +
                                    USERNAME_KEY +
                                    ":'"
                );
            }
        }

    }

    private void addGroup(String g) {
        groups.add(g);
    }

    private void addUsername(String u) {
        usernames.add(u);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("YamlPolicy[id:");
        sb.append(identify()).append(", groups:");
        for (final String group : getGroups()) {
            sb.append(group).append(" ");
        }
        sb.append("]");
        return sb.toString();
    }


    /**
     * Represents a match result with a decision result,
     */
    static class MatchedContext extends PairImpl<Boolean, ContextDecision> {
        MatchedContext(final Boolean matched, final ContextDecision decision) {
            super(matched, decision);
        }

        /**
         * Returns true if the context matched
         */
        public Boolean isMatched() {
            return getFirst();
        }

        /**
         * Returns the decision result
         */
        public ContextDecision getDecision() {
            return getSecond();
        }
    }

    static interface RuleConstructor {
        AclRule createRule(AclRuleBuilder prototype);
    }

    /**
     * returns an allow/reject decision for a specific rule within a type section, can return null indicating there was
     * no match. Format:
     * <pre>
     *     match:
     *       key: regex
     *       key2: [regexa, regexb]
     *     equals:
     *       key: value
     *     contains:
     *       key: value
     *       key2: [value1,value2]
     *     allow: action
     *     # or
     *     allow: [action1,action2]
     *     deny: action
     *     #or
     *     deny: [action1,action2]
     * </pre>
     */
    static class TypeRuleConstructor implements RuleConstructor {
        public static final String MATCH_SECTION = "match";
        public static final String EQUALS_SECTION = "equals";
        public static final String CONTAINS_SECTION = "contains";
        public static final String SUBSET_SECTION = "subset";
        public static final String ALLOW_ACTIONS = "allow";
        public static final String DENY_ACTIONS = "deny";
        static final Set<String> ALLOWED_CONTENT;

        static {
            HashSet<String> strings = new HashSet<>();
            Collections.addAll(
                    strings,
                    MATCH_SECTION,
                    EQUALS_SECTION,
                    CONTAINS_SECTION,
                    SUBSET_SECTION,
                    ALLOW_ACTIONS,
                    DENY_ACTIONS
            );
            ALLOWED_CONTENT = Collections.unmodifiableSet(strings);
        }

        Map<String, ?> ruleSection;
        Map<String, Map<String, ?>> matchSections = new HashMap<>();
        int index;
        YamlPolicy policy;
        ValidationSet validation;
        String type;


        TypeRuleConstructor(
                final String type,
                final Map<String, ?> ruleSection,
                ValidationSet validation,
                final int index,
                final YamlPolicy policy
        )
        {
            this.type=type;
            this.ruleSection = ruleSection;
            this.index = index;
            this.policy = policy;
            this.validation = validation;
            validate(validation);
        }

        private void validate(ValidationSet validation) {
            if(null==validation){
                return;
            }
            if(ruleSection.containsKey(DENY_ACTIONS)){
                final HashSet<String> actions = getDenyActions();
                if(null==actions) {
                    validation.addError(
                            policy.identify(),
                            identify() +
                            " Section '" +
                            DENY_ACTIONS +
                            ":' expected a String or a sequence of Strings, but was a " +
                            ruleSection.get(DENY_ACTIONS).getClass().getName()
                    );
                }else {
                    if (0 == actions.size()) {
                        logger.warn(policy.identify() + ": No actions defined in Deny section");

                        validation.addError(
                                policy.identify(),
                                identify() +
                                " Section '" + DENY_ACTIONS + ":' should not be empty."
                        );
                    }
                }
            }
            if(ruleSection.containsKey(ALLOW_ACTIONS)){
                final HashSet<String> actions = getAllowActions();
                if(null==actions) {
                    validation.addError(
                            policy.identify(),
                            identify() +
                            " Section '" + ALLOW_ACTIONS + ":' expected a String or a sequence of Strings, but was a "+
                            ruleSection.get(ALLOW_ACTIONS).getClass().getName()
                    );
                }else {
                    if (0 == actions.size()) {
                        logger.warn(policy.identify() + ": No actions defined in Deny section");

                        validation.addError(
                                policy.identify(),
                                identify()+
                                " Section '" + ALLOW_ACTIONS + ":' should not be empty."
                        );
                    }
                }
            }
            if (!ruleSection.containsKey(ALLOW_ACTIONS) && !ruleSection.containsKey(DENY_ACTIONS)) {

                validation.addError(
                        policy.identify(),
                        identify()+
                        " One of '" + ALLOW_ACTIONS + ":' or '"+DENY_ACTIONS+":' must be present."
                );
            }
            Map matchBody=null;
            String sectionName=null;
            if (isRuleSectionContains()) {
                sectionName=CONTAINS_SECTION;
                matchBody = (Map) ruleSection.get(CONTAINS_SECTION);
            } else if (isRuleSectionSubset()) {
                sectionName = SUBSET_SECTION;
                matchBody = (Map) ruleSection.get(SUBSET_SECTION);
            }else if(isRuleSectionMatch()){
                sectionName=MATCH_SECTION;
                matchBody = (Map) ruleSection.get(MATCH_SECTION);
            }else if(isRuleSectionEquals()){
                sectionName=EQUALS_SECTION;
                matchBody = (Map) ruleSection.get(EQUALS_SECTION);
            }
            if(matchBody!=null){
                if(matchBody.size()<1){

                    validation.addError(
                            policy.identify(),
                            identify()+
                            " Section '" + sectionName + ":' should not be empty."
                    );
                }else if (sectionName.equals(CONTAINS_SECTION) &&
                          (
                                  matchBody.size() != 1
                                  || !matchBody.containsKey( "tags")
                          )
                        ) {
                    validation.addError(
                            policy.identify(),
                            identify() +
                            " Section '" + CONTAINS_SECTION + ":' can only be applied to 'tags'."
                    );
                }
                if (matchBody.containsKey(ALLOW_ACTIONS) || matchBody.containsKey(DENY_ACTIONS)) {
                    validation.addError(
                            policy.identify(),
                            identify()+
                            " Section '" +
                            sectionName +
                            ":' should not contain '" +
                            ALLOW_ACTIONS +
                            ":' or '" +
                            DENY_ACTIONS +
                            ":'."
                    );
                }
            }
//            if(!isRuleSectionMatch() && !isRuleSectionContains() && !isRuleSectionEquals()) {
//                validation.addError(
//                        identify(),
//                        "One of '" +
//                        MATCH_SECTION +
//                        ":' or '" +
//                        CONTAINS_SECTION +
//                        ":' or '" +
//                        EQUALS_SECTION +
//                        ":' must be present."
//                );
//            }
        }

        @Override
        public String toString() {
            return (null!=policy?policy.identify()+" ":"")+identify();
        }

        private String identify() {
            return  "Type rule 'for: { "+type+ ": [...] }' entry at index ["+index+"]";
        }

        private static ConcurrentHashMap<String, Pattern> patternCache = new ConcurrentHashMap<String, Pattern>();

        private Pattern patternForRegex(final String regex) {
            if (!patternCache.containsKey(regex)) {
                Pattern compile = null;
                try {
                    compile = Pattern.compile(regex);
                } catch (Exception e) {
                    //invalid regex
                }
                if (null == compile) {
                    //create equality match regex
                    compile = Pattern.compile("^" + Pattern.quote(regex) + "$");
                }
                patternCache.putIfAbsent(regex, compile);
            }
            return patternCache.get(regex);
        }


        public AclRule createRule(AclRuleBuilder prototype) {
            AclRuleBuilder ruleBuilder = AclRuleBuilder.builder(prototype);

            final HashSet<String> allowActions = ruleSection.containsKey(ALLOW_ACTIONS)
                                                 ? getAllowActions()
                                                 : new HashSet<String>();
            final HashSet<String> denyActions = ruleSection.containsKey(DENY_ACTIONS)
                                                ? getDenyActions()
                                                : new HashSet<String>();
            ruleBuilder.sourceIdentityAppend("[rule: " + index + "]")
                       .allowActions(allowActions)
                       .denyActions(denyActions);

            //determine match section
            Map resourceMap;
            if(isRuleSectionMatch()) {
                resourceMap = (Map)ruleSection.get(MATCH_SECTION);
                ruleBuilder.regexResource(resourceMap);
            }else if(isRuleSectionContains()) {
                ruleBuilder.containsMatch(true);
                resourceMap = (Map)ruleSection.get(CONTAINS_SECTION);
                ruleBuilder.containsResource(resourceMap);
            } else if (isRuleSectionSubset()) {
                ruleBuilder.subsetMatch(true);
                resourceMap = (Map) ruleSection.get(SUBSET_SECTION);
                ruleBuilder.subsetResource(resourceMap);
            }else if(isRuleSectionEquals()){
                ruleBuilder.equalsMatch(true);
                resourceMap = (Map)ruleSection.get(EQUALS_SECTION);
                ruleBuilder.equalsResource(resourceMap);
            }else{
                resourceMap=null;
            }


            return ruleBuilder.build();
        }



        private HashSet<String> getAllowActions() {
            final HashSet<String> actions = new HashSet<String>();
            final Object actionsObj = ruleSection.get(ALLOW_ACTIONS);
            if (actionsObj instanceof String) {
                final String actionStr = (String) actionsObj;
                actions.add(actionStr);
            } else if (actionsObj instanceof List) {
                actions.addAll((List<String>) actionsObj);
            } else {
                return null;
            }
            return actions;
        }

        private HashSet<String> getDenyActions() {
            final HashSet<String> actions = new HashSet<String>();
            final Object actionsObj = ruleSection.get(DENY_ACTIONS);
            if (actionsObj instanceof String) {
                final String actionStr = (String) actionsObj;
                actions.add(actionStr);
            } else if (actionsObj instanceof List) {
                actions.addAll((List<String>) actionsObj);
            } else {
                return null;
            }
            return actions;
        }


        private boolean isRuleSectionContains() {
            return ruleSection.containsKey(CONTAINS_SECTION);
        }

        private boolean isRuleSectionSubset() {
            return ruleSection.containsKey(SUBSET_SECTION);
        }

        private boolean isRuleSectionEquals() {
            return ruleSection.containsKey(EQUALS_SECTION);
        }

        private boolean isRuleSectionMatch() {
            return ruleSection.containsKey(MATCH_SECTION);
        }

        private boolean validRuleSection(final Map section) {
            return null != section && section.size() > 0;
        }

        boolean ruleMatchesContainsSection(final Map<String, String> resource, final Map ruleSection) {
            final Map section = (Map) ruleSection.get(CONTAINS_SECTION);
            return validRuleSection(section) && predicateMatchRules(section, resource, true, new Converter<String,
                    Predicate>() {
                public Predicate convert(final String o) {
                    return new SetContainsPredicate(o);
                }
            });
        }

        boolean ruleMatchesEqualsSection(final Map<String, String> resource, final Map ruleSection) {
            final Map section = (Map) ruleSection.get(EQUALS_SECTION);

            return validRuleSection(section) && predicateMatchRules(section, resource, false, new Converter<String,
                    Predicate>() {
                public Predicate convert(final String o) {
                    return PredicateUtils.equalPredicate(o);
                }
            });
        }

        boolean ruleMatchesMatchSection(final Map<String, String> resource, final Map ruleSection) {
            final Map section = (Map) ruleSection.get(MATCH_SECTION);
            return validRuleSection(section) && predicateMatchRules(section, resource, true, new Converter<String,
                    Predicate>() {
                public Predicate convert(final String o) {
                    return new RegexPredicate(patternForRegex(o));
                }
            });
        }


        /**
         * Return true if all entries in the "match" map pass the predicate tests for the resource
         *
         * @param match                the set of matches to check
         * @param resource             the resource
         * @param allowListMatch       if true, allow the match value to be a list of values which much all pass the
         *                             test
         * @param predicateTransformer transformer to convert a String into a Predicate check
         */
        @SuppressWarnings("rawtypes")
        boolean predicateMatchRules(final Map match, final Map<String, String> resource, final boolean allowListMatch,
                final Converter<String, Predicate> predicateTransformer) {
            for (final Object o : match.entrySet()) {
                final Map.Entry entry = (Map.Entry) o;
                final String key = (String) entry.getKey();
                final Object test = entry.getValue();

                final boolean matched = applyTest(resource, allowListMatch, predicateTransformer, key, test);
                if (!matched) {
                    return false;
                }
            }
            return true;
        }

        /**
         * Return true if all predicate tests on a certain resource entry evaluate to true
         *
         * @param resource             the resource
         * @param allowListMatch       if true, allow the test to be a list of strings
         * @param predicateTransformer a Converter<S,Predicate> to convert String to Predicate test
         * @param key                  the resource attribute key to check
         * @param test                 test to apply, can be a String, or List of Strings if allowListMatch is true
         */
        boolean applyTest(final Map<String, String> resource, final boolean allowListMatch,
                final Converter<String, Predicate> predicateTransformer, final String key,
                final Object test) {

            final ArrayList<Predicate> tests = new ArrayList<Predicate>();
            if (allowListMatch && test instanceof List) {
                //must match all values
                for (final Object item : (List) test) {
                    final String s = (String) item;
                    tests.add(predicateTransformer.convert(s));
                }
            } else if (test instanceof String) {
                //match single test
                tests.add(predicateTransformer.convert((String) test));
            } else {
                //unexpected format, do not match
                logger.error(identify() + ": cannot evaluate unexpected type: " + test.getClass().getName());
                return false;
            }

            return PredicateUtils.allPredicate(tests).evaluate(resource.get(key));
        }

    }

    /**
     * evaluates to true if the input matches a regular expression
     */
    static class RegexPredicate implements Predicate {
        Pattern regex;

        RegexPredicate(final Pattern regex) {
            this.regex = regex;
        }

        public boolean evaluate(final Object o) {
            return o instanceof String && regex.matcher((String) o).matches();
        }

    }

    /**
     * Evaluates to true if the input is a string or collection of strings, and they are a superset of this object's
     * collection.
     */
    static class SetContainsPredicate implements Predicate {
        HashSet<String> items = new HashSet<String>();

        SetContainsPredicate(final Object item) {
            if (item instanceof String) {
                items.add((String) item);
            } else if (item instanceof List) {
                items.addAll((List<String>) item);
            } else {
                //unexpected, will reject everything
                items = null;
            }
        }

        public boolean evaluate(final Object o) {
            if (null == items || null == o) {
                return false;
            }
            final Collection input;
            if (o instanceof String) {
                final HashSet<String> hs = new HashSet<String>();
                //treat o as comma-seperated list of strings
                final String str = (String) o;
                final String[] split = str.split(",");
                for (final String s : split) {
                    hs.add(s.trim());
                }
                input = hs;
            } else if (o instanceof Collection) {
                input = (Collection) o;
            } else {
                return false;
            }
            return CollectionUtils.isSubCollection(items, input);
        }
    }

    /**
     * Returns decision for a resource and action, based on the "type" of the resource, and the rules defined in the
     * for: type: section of the policy def.
     */
    static class YamlRuleSetConstructor implements RuleSetConstructor {
        private String description = "Not Evaluated: ";
        Map policyDef;
        private final ConcurrentHashMap<String, RuleSetConstructor> typeContexts = new ConcurrentHashMap<>();
        TypeRuleSetConstructorFactory typeRuleSetConstructorFactory;
        private Map forsection;
        private ValidationSet validation;

        YamlRuleSetConstructor(
                final Map policyDef,
                final ValidationSet validation,
                final TypeRuleSetConstructorFactory typeRuleSetConstructorFactory
        )
        {
            this.policyDef = policyDef;
            this.typeRuleSetConstructorFactory = typeRuleSetConstructorFactory;
            this.validation=validation;
            initialize();
        }

        private void initialize() {
            //require description
            final Object descriptionValue = policyDef.get(DESCRIPTION_KEY);
            if (descriptionValue == null || !(descriptionValue instanceof String)) {
                throw new AclPolicySyntaxException("Policy is missing a description");
            }
            description = (String) descriptionValue;

            final Object forMap = policyDef.get(FOR_SECTION);

            //require for section is a map
            if (null == forMap) {
                throw new AclPolicySyntaxException("Required '" + FOR_SECTION + ":' section was not present");
            }
            if (!(forMap instanceof Map)) {
                throw new AclPolicySyntaxException("Expected '" + FOR_SECTION + ":' section to contain a map, " +
                        "but was [" + (forMap.getClass().getName()) + "].");
            }

            forsection = (Map) forMap;

            for (Object key : forsection.keySet()) {
                if (key instanceof String) {
                    String type = (String) key;
                    Object typeSection = forsection.get(key);
                    if(!(typeSection instanceof List)) {
                        throw new AclPolicySyntaxException("Expected '" + FOR_SECTION + ": { " + key + ": <...> }' section to " +
                                "contain a List, but was [" + (forMap.getClass().getName()) + "].");
                    }
                    List typeSectionList = (List) typeSection;
                    if(typeSectionList.size()<1){
                        throw new AclPolicySyntaxException("Section '" + FOR_SECTION + ": { " + key + ": [...] }' list should not be empty.");
                    }
                    typeContexts.putIfAbsent(
                            type,
                            typeRuleSetConstructorFactory.createRuleSetConstructor(
                                    type,
                                    typeSectionList
                            )
                    );
                }else{
                    throw new AclPolicySyntaxException("Section '" + FOR_SECTION + ":' key '"+key+":' was not a string.");
                }
            }
            if(forsection.size()<1){
                throw new AclPolicySyntaxException("Section '" + FOR_SECTION + ":' should not be empty.");
            }
        }

        public String toString() {
            return "Context: " + description;
        }

        @Override
        public Set<AclRule> createRules(final AclRuleBuilder prototype) {
            HashSet<AclRule> aclRules = new HashSet<>();
            for (Map.Entry<String, RuleSetConstructor> typeRules : typeContexts.entrySet()) {
                AclRuleBuilder builder = AclRuleBuilder.builder(prototype);

                String type = typeRules.getKey();
                builder.sourceIdentityAppend("[type:" + type + "]");
                builder.resourceType(type);
                RuleSetConstructor value = typeRules.getValue();
                aclRules.addAll(value.createRules(builder));
            }
            return aclRules;
        }
    }
}
