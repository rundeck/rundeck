package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.Username;
import com.dtolabs.rundeck.core.authorization.providers.*;
import com.dtolabs.rundeck.core.utils.Converter;
import com.dtolabs.rundeck.core.utils.PairImpl;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.collections.PredicateUtils;
import org.apache.log4j.Logger;

import javax.security.auth.Subject;
import java.io.PrintStream;
import java.security.Principal;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Evaluate ACL requests over a set of rules
 */
public class RuleEvaluator implements Authorization, AclRuleSetSource {
    private final static Logger logger = Logger.getLogger(RuleEvaluator.class);
    final private AclRuleSet rules;
    final private AclRuleSetSource source;

    private RuleEvaluator(final AclRuleSetSource ruleSetSource) {
        this.source = ruleSetSource;
        this.rules = null;
    }

    private RuleEvaluator(final AclRuleSet rules) {
        this.source = null;
        this.rules = rules;
    }

    public static RuleEvaluator createRuleEvaluator(final AclRuleSetSource ruleSetSource) {
        return new RuleEvaluator(ruleSetSource);
    }

    public static RuleEvaluator createRuleEvaluator(final AclRuleSet rules) {
        return new RuleEvaluator(rules);
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
                                subjectFrom(subject),
                                environment
                        )
        );
    }

    public List<AclRule> narrowContext(
            final AclRuleSet ruleSet,
            final AclSubject subject, final Set<Attribute> environment
    )
    {
        List<AclRule> matchedContexts = new ArrayList<>();
        for (final AclRule f : ruleSet.getRules()) {
            if (matchesContexts(f, subject, environment)) {
                matchedContexts.add(f);
            }
        }
        return matchedContexts;
    }

    private boolean matchesContexts(
            final AclRule f,
            final AclSubject subject,
            final Set<Attribute> environment
    )
    {
        long userMatchStart = System.currentTimeMillis();

        if (f.getEnvironment() != null) {
            final EnvironmentalContext environment1 = f.getEnvironment();
            if (!environment1.isValid()) {
                logger.warn(f.toString() + ": Context section not valid: " + environment1.toString());
            }
            if (!environment1.matches(environment)) {
                if (logger.isDebugEnabled()) {
                    logger.debug(f.toString() + ": environment not matched: " + environment1.toString());
                }
                return false;
            }
        } else if (null != environment && environment.size() > 0) {
            logger.debug(f.toString() + ": empty environment not matched");
            return false;
        }


        if (subject.getUsername() != null && f.getUsername() != null) {

            if (subject.getUsername().equals(f.getUsername())
                || matchesPattern(subject.getUsername(), f.getUsername())
                    ) {
                return true;
            } else if (f.getUsername() != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug(f.toString() + ": username not matched: " + f.getUsername());
                }
            }
        }


        if (subject.getGroups().size() > 0) {
            // no username matched, check groups.
            if (subject.getGroups().contains(f.getGroup())
                || matchesAnyPatterns(subject.getGroups(), f.getGroup())) {
                return true;
            } else if (subject.getGroups().size() > 0) {
                if (logger.isDebugEnabled()) {
                    logger.debug(f.toString() + ": group not matched: " + f.getGroup());
                }
            }
        }
        return false;
    }

    private boolean matchesAnyPatterns(final Set<String> groups, final String patternStr) {
        final Pattern pattern;
        try {
            pattern = Pattern.compile(patternStr);
        } catch (Exception e) {
            return false;
        }
        for (Object groupName : groups) {
            if (pattern.matcher(groupName.toString()).matches()) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesPattern(final String username, final String pattern) {
        try {
            return Pattern.compile(pattern).matcher(username).matches();
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    private AclSubject subjectFrom(final Subject subject) {
        if (null == subject) {
            throw new NullPointerException("subject is null");
        }
        Set<Username> userPrincipals = subject.getPrincipals(Username.class);
        final String username;
        if (userPrincipals.size() > 0) {
            Username usernamep = userPrincipals.iterator().next();
            username = usernamep.getName();
        } else {
            username = null;
        }
        Set<Group> groupPrincipals = subject.getPrincipals(Group.class);
        final Set<String> groupNames = new HashSet<>();
        if (groupPrincipals.size() > 0) {
            for (Group groupPrincipal : groupPrincipals) {
                groupNames.add(groupPrincipal.getName());
            }
        }
        return new AclSubject() {
            @Override
            public String getUsername() {
                return username;
            }

            @Override
            public Set<String> getGroups() {
                return groupNames;
            }
        };
    }

    @Override
    public Set<Decision> evaluate(
            final Set<Map<String, String>> resources,
            final Subject subject,
            final Set<String> actions,
            final Set<Attribute> environment
    )
    {
        Set<Decision> decisions = new HashSet<Decision>();
        long duration = 0;
        List<AclRule> matchedRules = narrowContext(getRuleSet(), subjectFrom(subject), environment);
        for (Map<String, String> resource : resources) {
            for (String action : actions) {
                final Decision decision = evaluate(
                        resource, subject, action, environment, matchedRules
                );
                duration += decision.evaluationDuration();
                decisions.add(decision);
            }
        }
        return decisions;
    }

    /**
     * Return the evaluation decision for the resource, subject, action, environment and contexts
     */
    private Decision evaluate(
            Map<String, String> resource, Subject subject,
            String action, Set<Attribute> environment, List<AclRule> matchedRules
    )
    {

        Decision decision = internalEvaluate(resource, subject, action, environment, matchedRules);
        logger.info(MessageFormat.format("Evaluating {0} ({1}ms)", decision, decision.evaluationDuration()));
        return decision;
    }


    private static Decision authorize(
            final boolean authorized, final String reason,
            final Explanation.Code reasonId, final Map<String, String> resource, final Subject subject,
            final String action, final Set<Attribute> environment, final long evaluationTime
    )
    {
        return createAuthorize(
                authorized, new Explanation() {

                    public Code getCode() {
                        return reasonId;
                    }

                    public void describe(PrintStream out) {
                        out.println(toString());
                    }

                    public String toString() {
                        return "\t" + reason + " => " + reasonId;
                    }
                }, resource, subject, action, environment, evaluationTime
        );
    }

    static Decision createAuthorize(
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
                    builder.append(": ");
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
    private Decision internalEvaluate(
            Map<String, String> resource, Subject subject, String action,
            Set<Attribute> environment, List<AclRule> matchingRules
    )
    {
        long start = System.currentTimeMillis();
        if (matchingRules.size() < 1) {
            return authorize(
                    false,
                    "No context matches subject or environment",
                    Explanation.Code.REJECTED_NO_SUBJECT_OR_ENV_FOUND,
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
        } else {
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
        }

        if (subject == null) {
            throw new IllegalArgumentException("Invalid subject, subject is null.");
        }
        if (action == null || action.length() <= 0) {
            return authorize(
                    false,
                    "No action provided.",
                    Explanation.Code.REJECTED_NO_ACTION_PROVIDED,
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

        //long contextIncludeStart = System.currentTimeMillis();
        boolean granted = false;
        boolean denied = false;
        for (AclRule rule : matchingRules) {
            final ContextDecision includes = ruleIncludesResourceAction(rule, resource, action);
            if (Explanation.Code.REJECTED_DENIED == includes.getCode()) {
                contextDecision = includes;
                denied = true;
                return createAuthorize(
                        false, contextDecision, resource, subject, action, environment,
                        System.currentTimeMillis() - start
                );
            } else if (includes.granted()) {
                contextDecision = includes;
                granted = true;
            }
            lastDecision = includes;
        }
        if (granted) {
            return createAuthorize(
                    true, contextDecision, resource, subject, action, environment,
                    System.currentTimeMillis() - start
            );
        }

        if (lastDecision == null) {
            return authorize(
                    false,
                    "No resource or action matched.",
                    Explanation.Code.REJECTED_NO_RESOURCE_OR_ACTION_MATCH,
                    resource,
                    subject,
                    action,
                    environment,
                    System.currentTimeMillis() - start
            );
        } else {
            return createAuthorize(
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

    private ContextDecision ruleIncludesResourceAction(
            final AclRule ctx,
            final Map<String, String> resource,
            final String action
    )
    {
        final ArrayList<ContextEvaluation> evaluations = new ArrayList<ContextEvaluation>();
        final Explanation.Code decision = includes(ctx, resource, action);
        evaluations.add(
                new ContextEvaluation(
                        decision,
                        MessageFormat.format("{0} {1} for action {2}", ctx, decision, action)
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
        if (rule.isRegexMatch()) {
            return ruleMatchesMatchSection(resource, rule)
                   ? allowOrDenyAction(rule, action)
                   : Explanation.Code.REJECTED;
        } else if (rule.isEqualsMatch()) {
            return ruleMatchesEqualsSection(resource, rule)
                   ? allowOrDenyAction(rule, action)
                   : Explanation.Code.REJECTED;
        } else if (rule.isContainsMatch()) {
            return ruleMatchesContainsSection(resource, rule)
                   ? allowOrDenyAction(rule, action)
                   : Explanation.Code.REJECTED;
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

    boolean ruleMatchesContainsSection(final Map<String, String> resource, final AclRule rule) {
        return validRuleSection(rule.getResource()) && predicateMatchRules(
                rule, resource, true, new Converter<String,
                        Predicate>()
                {
                    public Predicate convert(final String o) {
                        return new SetContainsPredicate(o);
                    }
                }
        );
    }

    boolean ruleMatchesEqualsSection(final Map<String, String> resource, final AclRule rule) {

        return validRuleSection(rule.getResource()) && predicateMatchRules(
                rule, resource, false, new Converter<String,
                        Predicate>()
                {
                    public Predicate convert(final String o) {
                        return PredicateUtils.equalPredicate(o);
                    }
                }
        );
    }

    private boolean validRuleSection(final Map section) {
        return null != section && section.size() > 0;
    }

    boolean ruleMatchesMatchSection(final Map<String, String> resource, final AclRule ruleSection) {
        return validRuleSection(ruleSection.getResource()) && predicateMatchRules(
                ruleSection, resource, true, new Converter<String,
                        Predicate>()
                {
                    public Predicate convert(final String o) {
                        return new RegexPredicate(patternForRegex(o));
                    }
                }
        );
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
     * @param match                the set of matches to check
     * @param resource             the resource
     * @param allowListMatch       if true, allow the match value to be a list of values which much all pass the
     *                             test
     * @param predicateTransformer transformer to convert a String into a Predicate check
     */
    @SuppressWarnings("rawtypes")
    boolean predicateMatchRules(
            final AclRule match, final Map<String, String> resource, final boolean allowListMatch,
            final Converter<String, Predicate> predicateTransformer
    )
    {
        for (final Object o : match.getResource().entrySet()) {
            final Map.Entry entry = (Map.Entry) o;
            final String key = (String) entry.getKey();
            final Object test = entry.getValue();

            final boolean matched = applyTest(match, resource, allowListMatch, predicateTransformer, key, test);
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
    boolean applyTest(
            final AclRule rule, final Map<String, String> resource, final boolean allowListMatch,
            final Converter<String, Predicate> predicateTransformer, final String key,
            final Object test
    )
    {

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
            logger.error(rule.getSourceIdentity() + ": cannot evaluate unexpected type: " + test.getClass().getName());
            return false;
        }

        return PredicateUtils.allPredicate(tests).evaluate(resource.get(key));
    }
}
