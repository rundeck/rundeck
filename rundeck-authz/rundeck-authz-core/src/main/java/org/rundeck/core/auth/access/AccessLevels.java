package org.rundeck.core.auth.access;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.rundeck.core.auth.AuthConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AccessLevels {

    public static AuthActions action(String action) {
        return or(Collections.singletonList(action));
    }

    public static AuthActions or(AuthActions a, AuthActions b) {
        return or(append(a.getActions(), b.getActions()));
    }

    private static List<String> append(
            final List<String> a, final List<String> b
    )
    {
        List<String> newList = new ArrayList<>(a);
        newList.addAll(b);
        return newList;
    }

    public static AuthActions or(String a, AuthActions actions) {
        return or(Collections.singletonList(a), actions);
    }

    public static AuthActions or(List<String> a, AuthActions actions) {
        if (nonEmpty(actions.getActions())) {
            return or(append(a, actions.getActions()));
        } else {
            throw new IllegalArgumentException("Cannot combine any and all access levels: "
                                               + actions
                                               + " and "
                                               + a);
        }
    }

    private static boolean nonEmpty(final List<String> list) {
        return list != null && list.size() > 0;
    }

    public static AuthActions or(AuthActions a, List<String> actions) {
        if (nonEmpty(a.getActions())) {
            return or(append(a.getActions(), actions));
        } else {
            throw new IllegalArgumentException("Cannot combine any and all access levels: "
                                               + a
                                               + " and "
                                               + actions);
        }
    }

    public static AuthActions or(String... args) {
        return or(Arrays.asList(args));
    }

    public static AuthActions or(List<String> args) {
        return new AnyAuth(args);
    }

    public static AuthActions withDescription(AuthActions actions, String description) {
        return new AnyAuth(actions.getActions(), description);
    }

    public static final AuthActions
            ALL_ADMIN = or(AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_OPS_ADMIN);
    public static final AuthActions APP_ADMIN = or(AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN);
    public static final AuthActions OPS_ADMIN = or(AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN);
    public static final AuthActions APP_CREATE = or(AuthConstants.ACTION_CREATE, APP_ADMIN);
    public static final AuthActions APP_READ = or(AuthConstants.ACTION_READ, APP_ADMIN);
    public static final AuthActions APP_UPDATE = or(AuthConstants.ACTION_UPDATE, APP_ADMIN);
    public static final AuthActions APP_DELETE = or(AuthConstants.ACTION_DELETE, APP_ADMIN);

    public static final AuthActions APP_READ_OR_VIEW =
            action(AuthConstants.ACTION_VIEW)
                    .or(AuthConstants.ACTION_READ)
                    .or(APP_ADMIN);

    @Getter
    @EqualsAndHashCode
    public static class AnyAuth
            implements AuthActions
    {

        private final List<String> actions;
        @EqualsAndHashCode.Exclude
        private final String description;

        public AnyAuth(final List<String> actions) {
            this(actions, null);
        }

        public AnyAuth(final List<String> actions, String description) {
            if (null == actions || actions.size() < 1) {
                throw new IllegalArgumentException("anyActions cannot be empty");
            }
            this.actions = actions;
            this.description = description;
        }

        public String getDescription() {
            if (null != this.description) {
                return this.description;
            } else if (actions != null && actions.size() > 0) {
                return actions.get(0);
            } else {
                return "(unknown)";
            }
        }

        @Override
        public String toString() {
            return "AnyAccess{" + getActions() + (description != null ? ": " + description : "") + "}";
        }
    }
}
