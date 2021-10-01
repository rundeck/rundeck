package org.rundeck.app.authorization

import groovy.transform.CompileStatic
import org.rundeck.core.auth.AuthConstants

@CompileStatic
class AccessLevels {
    static final AccessActions APP_ADMIN = any(
        AuthConstants.ACTION_ADMIN,
        AuthConstants.ACTION_APP_ADMIN
    )
    static final AccessActions OPS_ADMIN = any(
        AuthConstants.ACTION_ADMIN,
        AuthConstants.ACTION_OPS_ADMIN
    )
    static final AccessActions APP_CREATE = any(
        AuthConstants.ACTION_CREATE,
        AuthConstants.ACTION_ADMIN,
        AuthConstants.ACTION_APP_ADMIN,
        )
    static final AccessActions APP_READ = any(
        AuthConstants.ACTION_READ,
        AuthConstants.ACTION_ADMIN,
        AuthConstants.ACTION_APP_ADMIN
    )
    static final AccessActions APP_UPDATE = any(
        AuthConstants.ACTION_UPDATE,
        AuthConstants.ACTION_ADMIN,
        AuthConstants.ACTION_APP_ADMIN
    )
    static final AccessActions APP_DELETE = any(
        AuthConstants.ACTION_DELETE,
        AuthConstants.ACTION_ADMIN,
        AuthConstants.ACTION_APP_ADMIN
    )

    static final AccessActions APP_READ_OR_VIEW = any(
        AuthConstants.ACTION_VIEW,
        AuthConstants.ACTION_READ,
        AuthConstants.ACTION_ADMIN,
        AuthConstants.ACTION_APP_ADMIN,
        )

    static final AccessActions any(AccessActions a, AccessActions b) {
        if (a.anyActions && b.anyActions) {
            return any(a.anyActions + b.anyActions)
        } else {
            throw new IllegalArgumentException("Cannot combine any and all access levels: $a and $b")
        }
    }

    static final AccessActions any(List<String> a, AccessActions actions) {
        if (actions.anyActions) {
            return any(a + actions.anyActions)
        } else {
            throw new IllegalArgumentException("Cannot combine any and all access levels: $actions and $a")
        }
    }

    static final AccessActions any(AccessActions a, List<String> actions) {
        if (a.anyActions) {
            return any(a.anyActions + actions)
        } else {
            throw new IllegalArgumentException("Cannot combine any and all access levels: $a and $actions")
        }
    }

    static final AccessActions any(String... args) {
        return any(args.toList())
    }

    static class AnyAccess implements AccessActions {
        final List<String> requiredActions = null
        final List<String> anyActions

        AnyAccess(final List<String> anyActions) {
            this.anyActions = anyActions
        }

        @Override
        public String toString() {
            return "AnyAccess{$anyActions}";
        }
    }

    static class AllAccess implements AccessActions {
        final List<String> requiredActions
        final List<String> anyActions = null

        AllAccess(final List<String> requiredActions) {
            this.requiredActions = requiredActions
        }

        @Override
        public String toString() {
            return "AllAccess{$requiredActions}";
        }
    }

    static final AccessActions any(List<String> args) {
        return new AnyAccess(args)
    }

    static final AccessActions all(String... args) {
        return new AllAccess(args.toList())
    }
}
