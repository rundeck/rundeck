package org.rundeck.core.auth.app;

import org.rundeck.core.auth.AuthConstants;
import org.rundeck.core.auth.access.AuthActions;
import org.rundeck.core.auth.access.NamedAuthDefinition;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.rundeck.core.auth.access.AuthActionsUtil.action;
import static org.rundeck.core.auth.access.AuthActionsUtil.or;

/**
 * Access level constants
 */
public class RundeckAccess {

    public static final class General {
        public static final String AUTH_ANY_ADMIN = "anyAdmin";
        public static final AuthActions ALL_ADMIN = or(AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_OPS_ADMIN);

        public static final String AUTH_APP_ADMIN = "appAdmin";
        public static final AuthActions APP_ADMIN = or(AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN);

        public static final String AUTH_OPS_ADMIN = "opsAdmin";
        public static final AuthActions OPS_ADMIN = or(AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN);

        public static final String AUTH_APP_CREATE = "appCreate";
        public static final AuthActions APP_CREATE = or(AuthConstants.ACTION_CREATE, APP_ADMIN);

        public static final String AUTH_APP_READ = "appRead";
        public static final AuthActions APP_READ = or(AuthConstants.ACTION_READ, APP_ADMIN);

        public static final String AUTH_APP_UPDATE = "appUpdate";
        public static final AuthActions APP_UPDATE = or(AuthConstants.ACTION_UPDATE, APP_ADMIN);

        public static final String AUTH_APP_DELETE = "appDelete";
        public static final AuthActions APP_DELETE = or(AuthConstants.ACTION_DELETE, APP_ADMIN);

        public static final Map<String, AuthActions> NAMED_AUTH_ACTIONS;

        static {
            Map<String, AuthActions> named = new HashMap<String, AuthActions>() {{
                put(AUTH_ANY_ADMIN, ALL_ADMIN);
                put(AUTH_APP_ADMIN, APP_ADMIN);
                put(AUTH_OPS_ADMIN, OPS_ADMIN);
                put(AUTH_APP_CREATE, APP_CREATE);
                put(AUTH_APP_READ, APP_READ);
                put(AUTH_APP_UPDATE, APP_UPDATE);
                put(AUTH_APP_DELETE, APP_DELETE);
            }};
            NAMED_AUTH_ACTIONS = Collections.unmodifiableMap(named);
        }
    }

    public static final class System implements NamedAuthDefinition {
        public static final String NAME = "system";

        public static final String AUTH_CONFIGURE = "configure";
        public static final AuthActions APP_CONFIGURE = action(AuthConstants.ACTION_CONFIGURE).or(RundeckAccess.General.ALL_ADMIN);

        public static final String AUTH_OPS_ENABLE_EXECUTION = "opsEnableExecution";
        public static final AuthActions OPS_ENABLE_EXECUTION = action(AuthConstants.ACTION_ENABLE_EXECUTIONS).or(RundeckAccess.General.OPS_ADMIN);

        public static final String AUTH_OPS_DISABLE_EXECUTION = "opsDisableExecution";
        public static final AuthActions OPS_DISABLE_EXECUTION = action(AuthConstants.ACTION_DISABLE_EXECUTIONS).or(RundeckAccess.General.OPS_ADMIN);

        public static final String AUTH_READ_OR_ANY_ADMIN = "readOrAnyAdmin";
        public static final AuthActions READ_OR_ANY_ADMIN = action(AuthConstants.ACTION_READ).or(RundeckAccess.General.ALL_ADMIN);

        public static final String AUTH_READ_OR_OPS_ADMIN = "readOrOpsAdmin";
        public static final AuthActions READ_OR_OPS_ADMIN = action(AuthConstants.ACTION_READ).or(RundeckAccess.General.OPS_ADMIN);

        public static final Map<String, AuthActions> NAMED_AUTH_ACTIONS;

        static {
            NAMED_AUTH_ACTIONS = Collections.unmodifiableMap(new HashMap<String, AuthActions>() {{
                put(AUTH_CONFIGURE, APP_CONFIGURE);
                put(AUTH_OPS_DISABLE_EXECUTION, OPS_DISABLE_EXECUTION);
                put(AUTH_OPS_ENABLE_EXECUTION, OPS_ENABLE_EXECUTION);
                put(AUTH_READ_OR_ANY_ADMIN, READ_OR_ANY_ADMIN);
                put(AUTH_READ_OR_OPS_ADMIN, READ_OR_OPS_ADMIN);
                putAll(General.NAMED_AUTH_ACTIONS);
            }});
        }

        public String getName() {
            return NAME;
        }
        public Map<String, AuthActions> getDefinitions(){
            return NAMED_AUTH_ACTIONS;
        }
    }

    public static final class Project implements NamedAuthDefinition {
        public static final String NAME = "project";

        public static final String AUTH_APP_CONFIGURE = "appConfigure";
        public static final AuthActions APP_CONFIGURE = action(AuthConstants.ACTION_CONFIGURE).or(RundeckAccess.General.APP_ADMIN);

        public static final String AUTH_APP_DELETE_EXECUTION = "appDeleteExecution";
        public static final AuthActions APP_DELETE_EXECUTION = action(AuthConstants.ACTION_DELETE_EXECUTION).or(RundeckAccess.General.APP_ADMIN);

        public static final String AUTH_APP_EXPORT = "appExport";
        public static final AuthActions APP_EXPORT = action(AuthConstants.ACTION_EXPORT).or(RundeckAccess.General.APP_ADMIN);

        public static final String AUTH_APP_IMPORT = "appImport";
        public static final AuthActions APP_IMPORT = action(AuthConstants.ACTION_IMPORT).or(RundeckAccess.General.APP_ADMIN);

        public static final String AUTH_APP_PROMOTE = "appPromote";
        public static final AuthActions APP_PROMOTE = action(AuthConstants.ACTION_PROMOTE).or(RundeckAccess.General.APP_ADMIN);

        public static final Map<String, AuthActions> NAMED_AUTH_ACTIONS;

        static {
            Map<String, AuthActions> named = new HashMap<String, AuthActions>() {{
                put(AUTH_APP_CONFIGURE, APP_CONFIGURE);
                put(AUTH_APP_DELETE_EXECUTION, APP_DELETE_EXECUTION);
                put(AUTH_APP_EXPORT, APP_EXPORT);
                put(AUTH_APP_IMPORT, APP_IMPORT);
                put(AUTH_APP_PROMOTE, APP_PROMOTE);
                putAll(General.NAMED_AUTH_ACTIONS);

            }};
            NAMED_AUTH_ACTIONS = Collections.unmodifiableMap(named);
        }
        public String getName() {
            return NAME;
        }
        public Map<String, AuthActions> getDefinitions(){
            return NAMED_AUTH_ACTIONS;
        }
    }

    public static final class Adhoc implements NamedAuthDefinition {
        public static final String NAME = "adhoc";
        public static final Map<String, AuthActions> NAMED_AUTH_ACTIONS;

        static {
            Map<String, AuthActions> named = new HashMap<String, AuthActions>() {{
                put(General.AUTH_APP_READ, General.APP_READ);
            }};
            NAMED_AUTH_ACTIONS = Collections.unmodifiableMap(named);
        }
        public String getName() {
            return NAME;
        }
        public Map<String, AuthActions> getDefinitions(){
            return NAMED_AUTH_ACTIONS;
        }
    }

    public static final class Execution implements NamedAuthDefinition {
        public static final String NAME = "execution";

        public static final String AUTH_APP_READ_OR_VIEW = "readOrView";
        public static final AuthActions APP_READ_OR_VIEW = action(AuthConstants.ACTION_VIEW).or(AuthConstants.ACTION_READ).or(General.APP_ADMIN);

        public final static String AUTH_APP_KILL = "appKill";
        public final static AuthActions APP_KILL = or(AuthConstants.ACTION_KILL, General.APP_ADMIN);

        public final static String AUTH_APP_KILLAS = "appKillAs";
        public final static AuthActions APP_KILLAS = or(AuthConstants.ACTION_KILLAS, General.APP_ADMIN);

        public static final Map<String, AuthActions> NAMED_AUTH_ACTIONS;

        static {
            Map<String, AuthActions> named = new HashMap<String, AuthActions>() {{
                put(AUTH_APP_READ_OR_VIEW, APP_READ_OR_VIEW);
                put(General.AUTH_APP_DELETE, General.APP_DELETE);
                put(AUTH_APP_KILL, APP_KILL);
                put(AUTH_APP_KILLAS, APP_KILLAS);
                putAll(General.NAMED_AUTH_ACTIONS);
            }};
            NAMED_AUTH_ACTIONS = Collections.unmodifiableMap(named);
        }

        public String getName() {
            return NAME;
        }
        public Map<String, AuthActions> getDefinitions(){
            return NAMED_AUTH_ACTIONS;
        }
    }

}
