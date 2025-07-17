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

package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authorization.providers.*;

import javax.security.auth.Subject;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Evaluate ACL requests over a set of rules
 */
public class RuleEvaluator implements AclRuleSetAuthorization {
    final private AclRuleSet rules;
    final private AclRuleSetSource source;
    final private AclSubjectCreator aclSubjectCreator;

    private RuleEvaluator(final AclRuleSetSource ruleSetSource, AclSubjectCreator aclSubjectCreator) {
        this.source = ruleSetSource;
        this.rules = null;
        this.aclSubjectCreator = aclSubjectCreator;
    }

    private RuleEvaluator(final AclRuleSet rules, AclSubjectCreator aclSubjectCreator) {
        this.source = null;
        this.rules = rules;
        this.aclSubjectCreator = aclSubjectCreator;
    }

    public interface AclSubjectCreator {
        AclSubject createFrom(Subject subject);
    }

    public static RuleEvaluator createRuleEvaluator(final AclRuleSetSource ruleSetSource, AclSubjectCreator creator) {
        return new RuleEvaluator(ruleSetSource, creator);
    }

    public static RuleEvaluator createRuleEvaluator(final AclRuleSet rules, AclSubjectCreator creator) {
        return new RuleEvaluator(rules, creator);
    }

    @Override
    public Decision evaluate(
            final Map<String, String> resource,
            final Subject subject,
            final String action,
            final Set<Attribute> environment
    )
    {
        return evaluate(resource, subject, action, environment, narrowContext(
                getRuleSet(),
                aclSubjectCreator.createFrom(subject),
                environment
                        )
        );
    }

    public static List<AclRule> narrowContext(
            final AclRuleSet ruleSet,
            final AclSubject subject,
            final Set<Attribute> environment
    )
    {
        return ruleSet.getRules()
                      .stream()
                      .filter(new Predicate<AclRule>() {
                          @Override
                          public boolean test(final AclRule rule) {
                              return matchesContexts(rule, subject, environment);
                          }
                      })
                      .collect(Collectors.toList());

    }

    public static boolean matchesContexts(
            final AclRule rule,
            final AclSubject subject,
            final Set<Attribute> environment
    )
    {
        if (rule.getEnvironment() != null) {
            final EnvironmentalContext environment1 = rule.getEnvironment();
            if (!environment1.matches(environment)) {
                return false;
            }
        } else if (null != environment && environment.size() > 0) {
            return false;
        }

        boolean isBy = rule.isBy();
        if (subject.getUsername() != null && rule.getUsername() != null) {
            if (subject.getUsername().equals(rule.getUsername())
                || matchesPattern(subject.getUsername(), rule.getUsername())
            ) {
                return isBy;
            }
        }


        if (subject.getGroups() != null && subject.getGroups().size() > 0) {
            // no username matched, check groups.
            if (subject.getGroups().contains(rule.getGroup())
                || matchesAnyPatterns(subject.getGroups(), rule.getGroup())) {
                return isBy;
            }
        }

        if (subject.getUrn() != null && rule.getUrn() != null) {
            if (subject.getUrn().equals(rule.getUrn())) {
                return isBy;
            }
        }

        //rule urn match for user: urn against username
        if (subject.getUsername() != null && rule.getUrn() != null && rule.getUrn().startsWith("user:")) {
            if (("user:" + subject.getUsername()).equals(rule.getUrn())) {
                return isBy;
            }
        }
        //rule urn match for role: urn against groups
        if (subject.getGroups() != null && rule.getUrn() != null && rule.getUrn().startsWith("group:")) {
            if (subject
                    .getGroups()
                    .stream()
                    .map(a -> "group:" + a)
                    .collect(Collectors.toSet())
                    .contains(rule.getUrn())) {
                return isBy;
            }
        }
        return !isBy;
    }

    public static boolean matchesAnyPatterns(final Collection<String> groups, final String patternStr) {
        final Pattern pattern;
        try {
            pattern = Pattern.compile(patternStr);
        } catch (Exception e) {
            return false;
        }
        for (Object groupName : groups) {
            if (groupName != null && pattern.matcher(groupName.toString()).matches()) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesPattern(final String username, final String pattern) {
        try {
            return Pattern.compile(pattern).matcher(username).matches();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    @Override
    public Set<Decision> evaluate(
            final Set<Map<String, String>> resources,
            final Subject subject,
            final Set<String> actions,
            final Set<Attribute> environment
    )
    {
        Set<Decision> decisions = new HashSet<>();
        List<AclRule> matchedRules = narrowContext(getRuleSet(), aclSubjectCreator.createFrom(subject), environment);
        for (Map<String, String> resource : resources) {
            for (String action : actions) {
                final Decision decision = evaluate(
                        resource, subject, action, environment, matchedRules
                );
                decisions.add(decision);
            }
        }

        return decisions;
    }


    private static Decision reject(
            final String reason,
            final Explanation.Code reasonId, final Map<String, String> resource, final Subject subject,
            final String action, final Set<Attribute> environment, final long evaluationTime
    )
    {
        return createDecision(
                false,
                Explanation.with(reasonId, reason),
                resource, subject, action, environment, evaluationTime
        );
    }

    static Decision createDecision(
            final boolean authorized, final Explanation explanation,
            final Map<String, String> resource, final Subject subject,
            final String action, final Set<Attribute> environment, final long evaluationTime
    )
    {

        return new Decision() {
            private String representation;

            public boolean isAuthorized() {
                return authorized;
            }

            public Map<String, String> getResource() {
                return resource;
            }

            public String getAction() {
                return action;
            }

            public Set<Attribute> getEnvironment() {
                return environment;
            }

            public Subject getSubject() {
                return subject;
            }

            public String toString() {
                if (representation == null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Decision for: ");
                    builder.append("res<");
                    Iterator<Map.Entry<String, String>> riter = resource.entrySet().iterator();
                    while (riter.hasNext()) {
                        Map.Entry<String, String> s = riter.next();
                        builder.append(s.getKey()).append(':').append(s.getValue());
                        if (riter.hasNext()) {
                            builder.append(", ");
                        }
                    }

                    builder.append("> subject<");
                    Iterator<Principal> iter = subject.getPrincipals().iterator();
                    while (iter.hasNext()) {
                        Principal principal = iter.next();
                        builder.append(principal.getClass().getSimpleName());
                        builder.append(':');
                        builder.append(principal.getName());
                        if (iter.hasNext()) {
                            builder.append(' ');
                        }
                    }

                    builder.append("> action<");
                    builder.append(action);

                    builder.append("> env<");
                    Iterator<Attribute> eiter = environment.iterator();
                    while (eiter.hasNext()) {
                        Attribute a = eiter.next();
                        builder.append(a);
                        if (eiter.hasNext()) {
                            builder.append(", ");
                        }
                    }
                    builder.append(">");
                    builder.append(": authorized: ");
                    builder.append(isAuthorized());
                    builder.append(": \t");
                    builder.append(explanation.toString());

                    this.representation = builder.toString();
                }
                return this.representation;
            }

            public Explanation explain() {
                return explanation;
            }

            public long evaluationDuration() {
                return evaluationTime;
            }
        };
    }

    /**
     * @param resource      resource
     * @param subject       subject
     * @param action        action
     * @param environment   environment
     * @param matchingRules contexts
     *
     * @return decision
     */
    private Decision evaluate(
            Map<String, String> resource, Subject subject, String action,
            Set<Attribute> environment, List<AclRule> matchingRules
    )
    {
        long start = System.currentTimeMillis();
        if (matchingRules.isEmpty()) {
            return reject(
                    "No context matches subject or environment",
                    Explanation.Code.REJECTED,
                    resource,
                    subject,
                    action,
                    environment,
                    System.currentTimeMillis() - start
            );
        }
        if (resource == null) {
            throw new IllegalArgumentException(
                    "Resource does not identify any resource because it's an empty resource property or null."
            );
        }
        for (Map.Entry<String, String> entry : resource.entrySet()) {
            if (entry.getKey() == null) {
                throw new IllegalArgumentException("Resource definition cannot contain null property name.");
            }
            if (entry.getValue() == null) {
                throw new IllegalArgumentException(
                        "Resource definition cannot contain null value.  Corresponding key: " + entry.getKey()
                );
            }
        }

        if (subject == null) {
            throw new IllegalArgumentException("Invalid subject, subject is null.");
        }
        if (action == null || action.length() <= 0) {
            return reject(
                    "No action provided.",
                    Explanation.Code.REJECTED,
                    resource,
                    subject,
                    action,
                    environment,
                    System.currentTimeMillis() - start
            );
        }
        // environment can be null.
        if (environment == null) {
            environment = Collections.emptySet();
        }


        ContextDecision contextDecision = null;
        ContextDecision lastDecision = null;

        boolean granted = false;
        for (AclRule rule : matchingRules) {
            final ContextDecision includes = ruleIncludesResourceAction(rule, resource, action);
            if (Explanation.Code.REJECTED_DENIED == includes.getCode()) {
                return createDecision(
                        false, includes, resource, subject, action, environment,
                        System.currentTimeMillis() - start
                );
            }
            if (includes.granted()) {
                contextDecision = includes;
                granted = true;
            }
            lastDecision = includes;
        }
        if (granted) {
            return createDecision(
                    true, contextDecision, resource, subject, action, environment,
                    System.currentTimeMillis() - start
            );
        }

        if (lastDecision == null) {
            return reject(
                    "No resource or action matched.",
                    Explanation.Code.REJECTED,
                    resource,
                    subject,
                    action,
                    environment,
                    System.currentTimeMillis() - start
            );
        } else {
            return createDecision(
                    false,
                    lastDecision,
                    resource,
                    subject,
                    action,
                    environment,
                    System.currentTimeMillis() - start
            );
        }
    }

    public AclRuleSet getRuleSet() {
        return null != source ? source.getRuleSet() : rules;
    }

    private ContextDecision ruleIncludesResourceAction(
            final AclRule rule,
            final Map<String, String> resource,
            final String action
    )
    {
        final ArrayList<ContextEvaluation> evaluations = new ArrayList<ContextEvaluation>();
        final Explanation.Code decision = includes(rule, resource, action);
        evaluations.add(
                new ContextEvaluation(
                        decision,
                        MessageFormat.format("{0} {1} for action {2}", rule, decision, action)
                )
        );
        return new ContextDecision(decision, Explanation.Code.GRANTED == decision, evaluations);
    }

    public Explanation.Code includes(final AclRule rule, final Map<String, String> resource, final String action) {
        //evaluate type
        if (rule.getResourceType() != null) {
            String resType = resource.get("type");
            if (null == resType || !rule.getResourceType().equals(resType)) {
                return Explanation.Code.REJECTED;
            }
        }

        //evaluate match:
        boolean matched = true;
        boolean tested = false;
        if (rule.isRegexMatch()) {
            tested = true;
            matched &= ruleMatchesMatchSection(resource, rule);
        }
        if (rule.isEqualsMatch()) {
            tested = true;
            matched &= ruleMatchesEqualsSection(resource, rule);
        }
        if (rule.isContainsMatch()) {
            tested = true;
            matched &= ruleMatchesContainsSection(resource, rule);
        }
        if (rule.isSubsetMatch()) {
            tested = true;
            matched &= ruleMatchesSubsetSection(resource, rule);
        }
        if (tested) {
            return matched ? allowOrDenyAction(rule, action) : Explanation.Code.REJECTED;
        } else {
            //no resource matching defined, matches all resources of this type.
            return allowOrDenyAction(rule, action);
        }
    }

    private Explanation.Code allowOrDenyAction(final AclRule rule, final String action) {
        if (rule.getDenyActions().contains(action) || rule.getDenyActions().contains("*")) {
            return Explanation.Code.REJECTED_DENIED;
        } else if (rule.getAllowActions().contains(action) || rule.getAllowActions().contains("*")) {
            return Explanation.Code.GRANTED;
        } else {
            return Explanation.Code.REJECTED;
        }
    }

    /**
     * Evaluates to true if the input is a string or collection of strings, and they are a superset of this object's
     * collection.
     */
    static class SetContainsPredicate implements Predicate<String> {
        HashSet<String> items = new HashSet<String>();

        SetContainsPredicate(List<String> items) {
            this.items.addAll(items);
        }

        SetContainsPredicate(final String item) {
            items.add(item);
        }

        static boolean isSubset(String sub, Set<String> sup) {
            return sup.containsAll(getCollection(sub));
        }
        static boolean isSubset(Set<String> sub, String sup) {
            return getCollection(sup).containsAll(sub);
        }

        public boolean test(final String o) {
            return isSubset(items, o);
        }

        static Set<String> getCollection(final String str) {
            final HashSet<String> hs = new HashSet<>();
            //treat o as comma-seperated list of strings
            final String[] split = null != str ? str.split(",") : new String[]{};
            for (final String s : split) {
                hs.add(s.trim());
            }
            return hs;
        }
    }

    /**
     * Evaluates to true if the input is a string or collection of strings, and they are a subset of this object's
     * collection.
     */
    static class SetSubsetPredicate extends SetContainsPredicate implements Predicate<String> {
        SetSubsetPredicate(final List<String> item) {
            super(item);
        }

        SetSubsetPredicate(final String item) {
            super(item);
        }

        public boolean test(final String o) {
            if (o == null) {
                return true;
            }
            return isSubset(o, items);
        }
    }

    private Function<String, Predicate<String>> setContainsString = new Function<String, Predicate<String>>() {
        @Override
        public Predicate<String> apply(final String item) {
            return new SetContainsPredicate(item);
        }
    };
    private Function<List, Predicate<String>> setContainsList = new Function<List, Predicate<String>>() {
        @Override
        public Predicate<String> apply(final List items) {
            return new SetContainsPredicate(items);
        }
    };
    private Function<String, Predicate<String>> setSubsetString = new Function<String, Predicate<String>>() {
        @Override
        public Predicate<String> apply(final String item) {
            return new SetSubsetPredicate(item);
        }
    };
    private Function<List, Predicate<String>> setSubsetList = new Function<List, Predicate<String>>() {
        @Override
        public Predicate<String> apply(final List items) {
            return new SetSubsetPredicate(items);
        }
    };
    boolean ruleMatchesContainsSection(final Map<String, String> resource, final AclRule rule) {
        return validRuleSection(rule.getContainsResource())
               &&
               predicateMatchRules(
                       resource,
                       setContainsString,
                       setContainsList,
                       rule.getContainsResource(),
                       rule.getSourceIdentity()
               );
    }

    boolean ruleMatchesSubsetSection(final Map<String, String> resource, final AclRule rule) {
        return validRuleSection(rule.getSubsetResource())
               &&
               predicateMatchRules(
                       resource,
                       setSubsetString,
                       setSubsetList,
                       rule.getSubsetResource(),
                       rule.getSourceIdentity()
               );
    }

    boolean ruleMatchesEqualsSection(final Map<String, String> resource, final AclRule rule) {

        return validRuleSection(rule.getEqualsResource())
               &&
               predicateMatchRules(
                       resource,
                       new Function<String, Predicate<String>>() {
                           @Override
                           public Predicate<String> apply(final String o) {
                               return new Predicate<String>() {
                                   @Override
                                   public boolean test(final String anObject) {
                                       return o.equals(anObject);
                                   }
                               };
                           }
                       },
                       null, rule.getEqualsResource(), rule.getSourceIdentity()
               );
    }

    private boolean validRuleSection(final Map section) {
        return null != section && section.size() > 0;
    }

    boolean ruleMatchesMatchSection(final Map<String, String> resource, final AclRule ruleSection) {
        return validRuleSection(ruleSection.getRegexResource())
               &&
               predicateMatchRules(
                       resource,
                       new Function<String, Predicate<String>>() {
                           @Override
                           public Predicate<String> apply(final String o) {
                               return new RegexPredicate(RuleEvaluator.this.patternForRegex(o));
                           }
                       },
                       null, ruleSection.getRegexResource(), ruleSection.getSourceIdentity()
               );
    }


    /**
     * evaluates to true if the input matches a regular expression
     */
    static class RegexPredicate implements Predicate<String> {
        Pattern regex;

        RegexPredicate(final Pattern regex) {
            this.regex = regex;
        }

        public boolean test(final String o) {
            return o != null && regex.matcher(o).matches();
        }

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

    /**
     * Return true if all entries in the "match" map pass the predicate tests for the resource
     *
     * @param resource             the resource
     * @param predicateTransformer transformer to convert a String into a Predicate check
     * @param listpred
     * @param ruleResource
     * @param sourceIdentity
     */
    @SuppressWarnings("rawtypes")
    boolean predicateMatchRules(
            final Map<String, String> resource,
            final Function<String, Predicate<String>> predicateTransformer,
            final Function<List, Predicate<String>> listpred,
            final Map<String, Object> ruleResource, final String sourceIdentity
    )
    {
        for (final Object o : ruleResource.entrySet()) {
            final Map.Entry entry = (Map.Entry) o;
            final String key = (String) entry.getKey();
            final Object test = entry.getValue();

            final boolean matched = applyTest(resource, predicateTransformer, key, test, listpred, sourceIdentity);
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
     * @param stringPredicate a Converter<S,Predicate> to convert String to Predicate test
     * @param key                  the resource attribute key to check
     * @param test                 test to apply, can be a String, or List of Strings if allowListMatch is true
     * @param listpred
     * @param sourceIdentity
     */
    boolean applyTest(
            final Map<String, String> resource,
            final Function<String, Predicate<String>> stringPredicate,
            final String key,
            final Object test,
            final Function<List, Predicate<String>> listpred,
            final String sourceIdentity
    )
    {

        final ArrayList<Predicate<String>> tests = new ArrayList<>();
        if (listpred != null && test instanceof List) {
            //must match all values
            tests.add(listpred.apply((List) test));
        } else if (test instanceof String) {
            //match single test
            tests.add(stringPredicate.apply((String) test));
        } else {
            //unexpected format, do not match
            if (test != null) {
//                logger.error(sourceIdentity + ": cannot evaluate unexpected type: " + test.getClass().getName());
            } else {
//                logger.error(sourceIdentity + ": cannot evaluate: null value for key `" + key + "`");
            }
            return false;
        }
        String value = resource.get(key);
        return tests.stream().allMatch(new Predicate<Predicate<String>>() {
            @Override
            public boolean test(final Predicate<String> pred) {
                return pred.test(value);
            }
        });
    }

}
