package org.rundeck.core.auth.access;

import lombok.Getter;
import org.rundeck.core.auth.AuthConstants;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AccessLevels {
    public static AccessActions any(AccessActions a, AccessActions b) {
        if (nonEmpty(a.getAnyActions()) && nonEmpty(b.getAnyActions())) {
            return any(append(a.getAnyActions(), b.getAnyActions()));
        } else {
            throw new IllegalArgumentException("Cannot combine any and all access levels: " + a + " and " + b);
        }

    }

    private static List<String> append(
            final List<String> a, final List<String> b
    )
    {
        List<String> newList = new ArrayList<>(a);
        newList.addAll(b);
        return newList;
    }

    public static AccessActions any(String a, AccessActions actions) {
        return any(Collections.singletonList(a), actions);
    }

    public static AccessActions any(List<String> a, AccessActions actions) {
        if (nonEmpty(actions.getAnyActions())) {
            return any(append(a, actions.getAnyActions()));
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

    public static AccessActions any(AccessActions a, List<String> actions) {
        if (nonEmpty(a.getAnyActions())) {
            return any(append(a.getAnyActions(), actions));
        } else {
            throw new IllegalArgumentException("Cannot combine any and all access levels: "
                                               + a
                                               + " and "
                                               + actions);
        }

    }

    public static AccessActions any(String... args) {
        return any(Arrays.asList(args));
    }

    public static AccessActions any(List<String> args) {
        return new AnyAccess(args);
    }

    public static AccessActions all(String... args) {
        return new AllAccess(Arrays.asList(args));
    }


    public static final AccessActions ALL_ADMIN = any(AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_OPS_ADMIN);
    public static final AccessActions APP_ADMIN = any(AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN);
    public static final AccessActions OPS_ADMIN = any(AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN);
    public static final AccessActions APP_CREATE = any(AuthConstants.ACTION_CREATE, APP_ADMIN);
    public static final AccessActions APP_READ = any(AuthConstants.ACTION_READ, APP_ADMIN);
    public static final AccessActions APP_UPDATE = any(AuthConstants.ACTION_UPDATE, APP_ADMIN);
    public static final AccessActions APP_DELETE = any(AuthConstants.ACTION_DELETE, APP_ADMIN);
    public static final AccessActions APP_READ_OR_VIEW = any(
            Arrays.asList(AuthConstants.ACTION_VIEW, AuthConstants.ACTION_READ),
            APP_ADMIN
    );

    @Getter
    public static class AnyAccess
            implements AccessActions
    {

        private final List<String> requiredActions = null;
        private final List<String> anyActions;

        public AnyAccess(final List<String> anyActions) {
            this.anyActions = anyActions;
        }

        @Override
        public String toString() {
            return "AnyAccess{" + getAnyActions() + "}";
        }


    }

    @Getter
    public static class AllAccess
            implements AccessActions
    {

        private final List<String> requiredActions;
        private final List<String> anyActions = null;

        public AllAccess(final List<String> requiredActions) {
            this.requiredActions = requiredActions;
        }

        @Override
        public String toString() {
            return "AllAccess{" + getRequiredActions() + "}";
        }

    }
}
