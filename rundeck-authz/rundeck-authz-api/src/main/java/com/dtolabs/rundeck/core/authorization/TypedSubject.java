package com.dtolabs.rundeck.core.authorization;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

public class TypedSubject
        implements RuleEvaluator.AclSubjectCreator

{
    private Class<? extends Principal> userType;
    private Class<? extends Principal> groupType;
    private Class<? extends Principal> urnType;

    public TypedSubject(final Class<? extends Principal> userType, final Class<? extends Principal> groupType, final Class<? extends Principal> urnType) {
        this.userType = userType;
        this.groupType = groupType;
        this.urnType = urnType;
    }

    public static RuleEvaluator.AclSubjectCreator aclSubjectCreator(final Class<? extends Principal> userType, final Class<? extends Principal> groupType, final Class<? extends Principal> urnType){
        return new TypedSubject(userType, groupType, urnType);
    }

    @Override
    public AclSubject createFrom(final Subject subject) {
        if (null == subject) {
            throw new NullPointerException("subject is null");
        }
        Set<? extends Principal> userPrincipals = subject.getPrincipals(userType);
        final String username;
        if (userPrincipals.size() > 0) {
            Principal usernamep = userPrincipals.iterator().next();
            username = usernamep.getName();
        } else {
            username = null;
        }
        Set<? extends Principal> groupPrincipals = subject.getPrincipals(groupType);
        final Set<String> groupNames = new HashSet<>();
        if (groupPrincipals.size() > 0) {
            for (Principal groupPrincipal : groupPrincipals) {
                groupNames.add(groupPrincipal.getName());
            }
        }

        Set<? extends Principal> urnsPrincipals = subject.getPrincipals(urnType);
        final String urnNames;
        if (urnsPrincipals.size() > 0) {
            Principal urnName = urnsPrincipals.iterator().next();
            urnNames = urnName.getName();
        } else {
            urnNames = null;
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

            @Override
            public String getUrn() {
                return urnNames;
            }
        };
    }
}
