/*
 * Copyright 2017 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.*;
import com.dtolabs.rundeck.core.authorization.providers.yaml.model.ACLPolicyDoc;
import com.dtolabs.rundeck.core.authorization.providers.yaml.model.YamlPolicyDocConstructor;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.error.YAMLException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * @author greg
 * @since 3/20/17
 */
public class YamlParsePolicy implements Policy {
    public static final String BY_SECTION = "by";
    public static final String USERNAME_KEY = "username";
    public static final String GROUP_KEY = "group";
    ACLPolicyDoc policyDoc;
    String sourceIdent;
    int sourceIndex;
    ValidationSet validation;
    private YamlParsePolicy.YamlEnvironmentalContext environment;

    private Set<String> usernames = new HashSet<String>();
    private Set<String> groups = new HashSet<String>();
    private Set<AclRule> rules = new HashSet<>();

    private YamlParsePolicy(
            final Set<Attribute> context,
            final ACLPolicyDoc policyDoc,
            final String sourceIdent,
            final int sourceIndex,
            ValidationSet validation
    )
    {
        this.policyDoc = policyDoc;
        this.sourceIdent = sourceIdent;
        this.sourceIndex = sourceIndex;
        this.validation = validation;
        validate();
        parseEnvironment(context);
        parseByClause();
        enumerateRules();
    }

    private Set<AclRule> createRules(final AclRuleBuilder proto) {
        HashSet<AclRule> aclRules = new HashSet<>();
        for (Map.Entry<String, List<ACLPolicyDoc.TypeRule>> typeRules : policyDoc.getFor()
//                                                                                 .getRuleSets()
                                                                                 .entrySet()) {

            AclRuleBuilder builder = AclRuleBuilder.builder(proto);

            String type = typeRules.getKey();
            builder.sourceIdentityAppend("[type:" + type + "]");
            builder.resourceType(type);
            List<ACLPolicyDoc.TypeRule> value = typeRules.getValue();

            aclRules.addAll(createRules(type, value, builder));
        }
        return aclRules;
    }

    private Set<? extends AclRule> createRules(
            final String type,
            final List<ACLPolicyDoc.TypeRule> typeRules,
            final AclRuleBuilder proto
    )
    {
        HashSet<AclRule> aclRules = new HashSet<>();
        int i = 1;
        for (ACLPolicyDoc.TypeRule typeRule : typeRules) {
            AclRuleBuilder builder = AclRuleBuilder.builder(proto);
            aclRules.add(createRule(type, i++, typeRule, builder));
        }
        return aclRules;
    }

    private AclRule createRule(
            final String type,
            final int index,
            final ACLPolicyDoc.TypeRule typeRule,
            final AclRuleBuilder prototype
    )
    {
        AclRuleBuilder ruleBuilder = AclRuleBuilder.builder(prototype);
        Object allow = typeRule.getAllow();
        Object deny = typeRule.getDeny();
        final Set<String> allowActions = null != typeRule.getAllow() ? typeRule.getAllowActions() : new HashSet<>();
        final Set<String> denyActions = null != typeRule.getDeny() ? typeRule.getDenyActions() : new HashSet<>();
        ruleBuilder.sourceIdentityAppend("[rule: " + index + "]")
                   .allowActions(allowActions)
                   .denyActions(denyActions)

                   //add resource match sections
                   .regexResource(typeRule.getMatch())
                   .containsResource(typeRule.getContains())
                   .subsetResource(typeRule.getSubset())
                   .equalsResource(typeRule.getEquals());


        return ruleBuilder.build();
    }

    private void enumerateRules() {
        String description = policyDoc.getDescription();
        AclRuleBuilder envProto = AclRuleBuilder.builder()
                                                .environment(environment.toBasic())
                                                .description(description)
                                                .sourceIdentity(sourceIdent);

        for (String username : usernames) {
            AclRuleBuilder ruleBuilder = AclRuleBuilder.builder(envProto).username(username);
            rules.addAll(createRules(ruleBuilder));
        }
        for (String group : groups) {
            AclRuleBuilder ruleBuilder = AclRuleBuilder.builder(envProto).group(group);
            rules.addAll(createRules(ruleBuilder));
        }
    }

    private void parseByClause() {

        final Object u = policyDoc.getBy().getUsername();
        final Object g = policyDoc.getBy().getGroup();

        if (null != u) {
            if (u instanceof String) {
                addUsername((String) u);
            } else if (u instanceof Collection) {
                for (final Object o : (Collection) u) {
                    if (o instanceof String) {
                        addUsername((String) o);
                    } else {
                        throw new AclPolicySyntaxException(
                                "Section '" +
                                USERNAME_KEY +
                                ":' should contain only Strings, but saw a: " +
                                o.getClass().getName()
                        );
                    }
                }
            } else {
                throw new AclPolicySyntaxException(
                        "Section '" +
                        USERNAME_KEY +
                        ":' should be a list or a String, but it was: " +
                        u.getClass().getName()
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
                    } else {
                        throw new AclPolicySyntaxException(
                                "Section '" +
                                GROUP_KEY +
                                ":' should contain only Strings, but saw a: " +
                                o.getClass().getName()
                        );
                    }
                }
            } else {
                throw new AclPolicySyntaxException(
                        "Section '" +
                        GROUP_KEY +
                        ":' should be a list or a String, but it was: " +
                        g.getClass().getName()
                );
            }
        }
        if (groups.size() < 1 && usernames.size() < 1) {
            if (null != validation) {
                validation.addError(
                        sourceIdent,
                        "Section '" + BY_SECTION +
                        ":' is not valid: " +
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

    private void validate() {
        if (null == policyDoc.getBy()) {
            throw new AclPolicySyntaxException(
                    "Required 'by:' section was not present"
            );
        }
        if (null == policyDoc.getBy().getGroup() && null == policyDoc.getBy().getUsername()) {

            throw new AclPolicySyntaxException(
                    "Section '" + BY_SECTION +
                    ":' is not valid: " +
                    " it must contain '" +
                    GROUP_KEY +
                    ":' and/or '" +
                    USERNAME_KEY +
                    ":'"
            );
        }
        if (null == policyDoc.getFor()) {
            throw new AclPolicySyntaxException(
                    "Required 'for:' section was not present"
            );
        } else if (policyDoc.getFor().isEmpty()) {
            throw new AclPolicySyntaxException(
                    "Section 'for:' should not be empty"
            );
        }
        //for section resources should not contain 'allow' or 'deny'
        HashSet<String> verify = new HashSet<>(Arrays.asList("allow", "deny"));
        for (String type : policyDoc.getFor().keySet()) {
            Map<String, List<ACLPolicyDoc.TypeRule>> aFor = policyDoc.getFor();
            List<ACLPolicyDoc.TypeRule> typeRules = aFor.get(type);
            if (typeRules.size() < 1) {
                throw new AclPolicySyntaxException(
                        String.format("Type rule 'for: { %s: [...] }' list should not be empty.", type)
                );
            }
            int typeIndex = 1;
            for (ACLPolicyDoc.TypeRule typeRule : typeRules) {
                validateRule(type, typeIndex, typeRule.getAllow(), "allow");
                validateRule(type, typeIndex, typeRule.getDeny(), "deny");

                if (typeRule.isEmpty()) {
                    throw new AclPolicySyntaxException(
                            String.format(
                                    "Type rule 'for: { %s: [...] }' entry at index [%d] One of 'allow:' or 'deny:' " +
                                    "must be present.",
                                    type,
                                    typeIndex
                            )
                    );

                }
                verifyTypeResourceKeys(verify, type, typeIndex, typeRule.getContains(), "contains", "tags");
                verifyTypeResourceKeys(verify, type, typeIndex, typeRule.getEquals(), "equals", null);
                verifyTypeResourceKeys(verify, type, typeIndex, typeRule.getMatch(), "match", null);
                verifyTypeResourceKeys(verify, type, typeIndex, typeRule.getSubset(), "subset", null);
                typeIndex++;
            }
        }
        if (null == policyDoc.getDescription()) {
            throw new AclPolicySyntaxException(
                    "Policy is missing a description"
            );
        }
    }

    private void validateRule(
            final String type,
            final int typeIndex, final Object grant, final String grantName
    )
    {
        if (grant == null) {
            return;
        }
        if (grant instanceof List) {
            List g = (List) grant;
            if (g.size() < 1) {
                throw new AclPolicySyntaxException(
                        String.format(
                                "Type rule 'for: { %s: [...] }' entry at index [%d] Section '%s:' should not be empty",
                                type,
                                typeIndex,
                                grantName
                        )
                );
            }
        }else if(!(grant instanceof String)) {

            throw new AclPolicySyntaxException(
                    String.format(
                            "Type rule 'for: { %s: [...] }' entry at index [%d] Section '%s:' expected a " +
                            "String or a sequence of Strings, but was a %s",
                            type,
                            typeIndex,
                            grantName,
                            grant.getClass().getName()
                    )
            );
        }
    }

    private void verifyTypeResourceKeys(
            final HashSet<String> verify,
            final String type,
            final int typeIndex,
            final Map<String, Object> resource,
            final String name,
            final String checkfor
    )
    {
        if (resource == null) {
            return;
        }
        if (resource.size() == 0) {

            throw new AclPolicySyntaxException("Type rule 'for: { " +
                                               type +
                                               ": [...] }\' entry at index [" +
                                               typeIndex +
                                               "] Section " +
                                               "'" + name + ":' should not be empty.");
        }
        List<String> collect = resource.keySet().stream()
                                       .filter(new Predicate<String>() {
                                           @Override
                                           public boolean test(final String o) {
                                               return verify.contains(o);
                                           }
                                       })
                                       .collect(Collectors.toList());

        //resource should not have null entries
        for (Map.Entry<String, Object> stringObjectEntry : resource.entrySet()) {
            if(stringObjectEntry.getValue()==null){

                throw new AclPolicySyntaxException("Type rule 'for: { " +
                        type +
                        ": [...] }\' entry at index [" +
                        typeIndex +
                        "] Section " +
                        "'" + name + ":' value for key: '" + stringObjectEntry.getKey() + "' cannot be null");
            }
        }

        if (collect.size() > 0) {
            throw new AclPolicySyntaxException("Type rule 'for: { " +
                                               type +
                                               ": [...] }\' entry at index [" +
                                               typeIndex +
                                               "] Section " +
                                               "'" + name + ":' should not contain 'allow:' or 'deny:'");
        }
        if (checkfor != null) {

            List<String> collect2 = resource.keySet().stream()
                                            .filter(new Predicate<String>() {
                                                @Override
                                                public boolean test(final String s) {
                                                    return !checkfor.equals(s);
                                                }
                                            })
                                            .collect(Collectors.toList());


            if (collect2.size() > 0) {
                throw new AclPolicySyntaxException("Type rule 'for: { " +
                                                   type +
                                                   ": [...] }\' entry at index [" +
                                                   typeIndex +
                                                   "] Section " +
                                                   "'" + name + ":' can only be applied to: '" + checkfor + "'");
            }
        }
    }

    private void parseEnvironment(final Set<Attribute> forcedContext) {
        ACLPolicyDoc.Context context = policyDoc.getContext();
        if (null != forcedContext) {
            if (null != context) {
                throw new AclPolicySyntaxException(
                        "Context section should not be specified, it is already set to: " +
                        AuthorizationUtil.contextAsString(forcedContext)
                );
            }
            environment = new YamlParsePolicy.YamlEnvironmentalContext(EnvironmentalContext.URI_BASE, forcedContext);
        } else if (null == context) {
            throw new AclPolicySyntaxException("Required 'context:' section was not present");
        } else {
            if (null != context.getProject() && null != context.getApplication() ||
                null == context.getProject() && null == context.getApplication()) {
                throw new AclPolicySyntaxException(
                        "Context section is not valid: " +
                        context +
                        ", it should have only one entry: 'application:' or 'project:'"
                );
            }
            environment = new YamlParsePolicy.YamlEnvironmentalContext(EnvironmentalContext.URI_BASE, context);
        }
        if (!environment.isValid()) {
            throw new AclPolicySyntaxException(
                    "Context section is not valid: " +
                    context +
                    environment.getValidation()
            );
        }
    }


    public static Policy createYamlPolicy(
            final Set<Attribute> forcedContext,
            final ACLPolicyDoc yamlDoc,
            final String ident,
            final int index,
            final ValidationSet validation
    )
    {
        return new YamlParsePolicy(forcedContext, yamlDoc, ident, index, validation);
    }

    @Override
    public AclRuleSet getRuleSet() {

        return new AclRuleSetImpl(rules);
    }

    @Override
    public Set<String> getUsernames() {
        return usernames;
    }


    @Override
    public Set<String> getGroups() {
        return groups;
    }

    @Override
    public String getDescription() {
        return policyDoc.getDescription();
    }

    @Override
    public EnvironmentalContext getEnvironment() {
        return environment.toBasic();
    }

    static private class YamlEnvironmentalContext {
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
            } else {
                Map.Entry<URI, String> next = matcher.entrySet().iterator().next();
                URI key = next.getKey();
                String value = next.getValue();
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
         *
         * @param uriPrefix prefix
         * @param ctx       context
         */
        YamlEnvironmentalContext(final String uriPrefix, final Set<Attribute> ctx) {
            for (Attribute attribute : ctx) {
                if (attribute.getProperty().toString().startsWith(uriPrefix)) {
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
            valid = matcher.size() >= 1;

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

        YamlEnvironmentalContext(final String uriPrefix, final ACLPolicyDoc.Context ctx) {
            boolean invalidentry = false;
            ArrayList<String> errors = new ArrayList<String>();
            final String key;
            final String value;
            if (null != ctx.getProject()) {
                key = "project";
                value = ctx.getProject();
            } else {
                key = "application";
                value = ctx.getApplication();
            }
            URI uri;
            try {
                uri = new URI(uriPrefix + key);
                matcher.put(uri, value);
                Pattern compile = Pattern.compile(value);
                matcherRegex.put(uri, compile);
            } catch (URISyntaxException e) {
                errors.add("Context section: " + key + ": invalid URI: " + e.getMessage());
                invalidentry = true;
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
                           ", validation='" +

                           getValidation() + '\'' +
                           '}');
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

    public static Iterable<ACLPolicyDoc> documentIterable(final Iterable<Object> iterator) {
        return documentIterable(iterator.iterator());
    }

    public static Iterable<ACLPolicyDoc> documentIterable(final Iterator<Object> iterator) {
        return () -> new Iterator<ACLPolicyDoc>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public ACLPolicyDoc next() {
                Object next = iterator.next();

                if (next == null) {
                    return null;
                }

                return (ACLPolicyDoc) next;
            }

        };
    }

    static YamlPolicyCollection.YamlSourceLoader<ACLPolicyDoc> loader(
            final YamlSource source1,
            final ValidationSet validation
    )
    {
        return new YamlPolicyCollection.YamlSourceLoader<ACLPolicyDoc>() {
            @Override
            public Iterable<ACLPolicyDoc> loadAll() throws IOException {
                final Yaml yaml = new Yaml(new YamlPolicyDocConstructor());
                Iterable<ACLPolicyDoc> objects = source1.loadAll(yaml);
                Iterator<ACLPolicyDoc> iterator = objects.iterator();
                return documentIterable(iterator);
            }

            public Iterable<ACLPolicyDoc> documentIterable(final Iterator<ACLPolicyDoc> iterator) {
                return new Iterable<ACLPolicyDoc>() {
                    @Override
                    public Iterator<ACLPolicyDoc> iterator() {
                        return new Iterator<ACLPolicyDoc>() {
                            int index = 0;

                            @Override
                            public boolean hasNext() {
                                return iterator.hasNext();
                            }

                            @Override
                            public ACLPolicyDoc next() {
                                Object next = null;
                                index++;
                                try {
                                    next = iterator.next();
                                } catch (ConstructorException e) {
                                    if (null != validation) {
                                        validation.addError(
                                                currentIdentity(),
                                                "Error parsing the policy document: " +extractSyntaxError(e.getCause().getMessage())
                                        );
                                    }
                                    return null;
                                } catch (YAMLException e) {
                                    if (null != validation) {
                                        validation.addError(
                                                currentIdentity(),
                                                "Error parsing the policy document: " + e.getMessage()
                                        );
                                    }
                                    return null;
                                }
                                if (next == null) {
                                    return null;
                                }
                                if (!(next instanceof ACLPolicyDoc)) {
                                    if (null != validation) {
                                        validation.addError(
                                                currentIdentity(),
                                                "Expected a YamlPolicyDoc document, but was type: " + next.getClass()
                                        );
                                    }
                                    return null;
                                }
                                return (ACLPolicyDoc) next;
                            }

                            private String currentIdentity() {
                                return source1.getIdentity() + "[" + index + "]";
                            }
                        };
                    }
                };
            }

            private String extractSyntaxError(String error){
                if(error != null) {
                    Pattern pattern = Pattern.compile("Unable to find property\\s(.+)\\son class");
                    Matcher matcher = pattern.matcher(error);
                    if (matcher.find() && null != matcher.group(1)) {
                        return "Unknown property: " + matcher.group(1);
                    }
                }
                return error;

            }

            @Override
            public void close() throws IOException {
                source1.close();
            }
        };
    }

    static YamlPolicyCollection.YamlPolicyCreator<ACLPolicyDoc> creator(
            final Set<Attribute> forcedContext,
            final ValidationSet validation
    )
    {
        return new YamlPolicyCollection.YamlPolicyCreator<ACLPolicyDoc>() {
            @Override
            public Policy createYamlPolicy(
                    final ACLPolicyDoc policyInput,
                    final String sourceIdent,
                    final int sourceIndex
            )
                    throws AclPolicySyntaxException
            {
                return YamlParsePolicy.createYamlPolicy(
                        forcedContext,
                        policyInput,
                        sourceIdent,
                        sourceIndex,
                        validation
                );
            }
        };
    }

}
