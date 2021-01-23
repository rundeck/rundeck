package com.dtolabs.rundeck.core.authorization;

import com.dtolabs.rundeck.core.authentication.Group;
import com.dtolabs.rundeck.core.authentication.Urn;
import com.dtolabs.rundeck.core.authentication.Username;

import javax.security.auth.Subject;
import java.util.HashSet;
import java.util.Set;

/**
 * encapsulates Subject and exposes user and roles
 */
public class SubjectUserAndRoles
        implements UserAndRoles
{
    private final Subject subject;

    public SubjectUserAndRoles(Subject subject) {
        this.subject = subject;
    }

    @Override
    public String getUsername() {
        Set<Username> principals = subject.getPrincipals(Username.class);
        if (principals.size() > 0) {
            return principals.iterator().next().getName();
        }
        return null;
    }

    @Override
    public Set<String> getRoles() {
        Set<Group> principals = subject.getPrincipals(Group.class);
        Set<String> roles = new HashSet<>();
        if (principals.size() > 0) {
            for (Group principal : principals) {
                roles.add(principal.getName());
            }
        }
        return roles;
    }

    @Override
    public String getUrn() {
        Set<Urn> principals = subject.getPrincipals(Urn.class);
        if (principals.size() > 0) {
            return principals.iterator().next().getName();
        }
        return null;
    }

    protected Subject getSubject() {
        return subject;
    }
}
