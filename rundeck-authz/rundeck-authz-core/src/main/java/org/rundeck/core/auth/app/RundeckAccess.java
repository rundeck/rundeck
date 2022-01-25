package org.rundeck.core.auth.app;

import lombok.Getter;
import org.rundeck.core.auth.AuthConstants;
import org.rundeck.core.auth.AuthResources;
import org.rundeck.core.auth.access.AuthActions;
import org.rundeck.core.auth.access.NamedAuthDefinition;

import java.util.*;

import static org.rundeck.core.auth.access.AuthActionsUtil.action;
import static org.rundeck.core.auth.access.AuthActionsUtil.or;

/**
 * Access level constants
 */
public class RundeckAccess {

    public static final class General {
        public static final String AUTH_ANY_ADMIN = "anyAdmin";
        /**
         * Any admin level
         */
        public static final AuthActions
                ALL_ADMIN =
                or(AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN, AuthConstants.ACTION_OPS_ADMIN);

        public static final String AUTH_APP_ADMIN = "appAdmin";
        /**
         * Admin or App Admin
         */
        public static final AuthActions APP_ADMIN = or(AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_APP_ADMIN);

        public static final String AUTH_OPS_ADMIN = "opsAdmin";
        /**
         * Admin or Ops Admin
         */
        public static final AuthActions OPS_ADMIN = or(AuthConstants.ACTION_ADMIN, AuthConstants.ACTION_OPS_ADMIN);

        public static final String AUTH_APP_CREATE = "appCreate";
        /**
         * Create or AppAdmin
         */
        public static final AuthActions APP_CREATE = or(AuthConstants.ACTION_CREATE, APP_ADMIN);

        public static final String AUTH_APP_READ = "appRead";
        /**
         * Read or AppAdmin
         */
        public static final AuthActions APP_READ = or(AuthConstants.ACTION_READ, APP_ADMIN);

        public static final String AUTH_APP_UPDATE = "appUpdate";
        /**
         * Update or AppAdmin
         */
        public static final AuthActions APP_UPDATE = or(AuthConstants.ACTION_UPDATE, APP_ADMIN);

        public static final String AUTH_APP_DELETE = "appDelete";
        /**
         * Delete or AppAdmin
         */
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

    @Getter
    public static final class System
            implements NamedAuthDefinition
    {
        public static final String GROUP = "system";
        public static final String TYPE = "app.system";

        public static final String AUTH_CONFIGURE = "configure";
        /**
         * Configure or all admin
         */
        public static final AuthActions
                APP_CONFIGURE =
                action(AuthConstants.ACTION_CONFIGURE).or(RundeckAccess.General.ALL_ADMIN);

        public static final String AUTH_OPS_ENABLE_EXECUTION = "opsEnableExecution";
        /**
         * Enable executions or ops admin
         */
        public static final AuthActions
                OPS_ENABLE_EXECUTION =
                action(AuthConstants.ACTION_ENABLE_EXECUTIONS).or(RundeckAccess.General.OPS_ADMIN);

        public static final String AUTH_OPS_DISABLE_EXECUTION = "opsDisableExecution";
        /**
         * Disable execution or ops admin
         */
        public static final AuthActions
                OPS_DISABLE_EXECUTION =
                action(AuthConstants.ACTION_DISABLE_EXECUTIONS).or(RundeckAccess.General.OPS_ADMIN);

        public static final String AUTH_READ_OR_ANY_ADMIN = "readOrAnyAdmin";
        /**
         * Read or any admin
         */
        public static final AuthActions
                READ_OR_ANY_ADMIN =
                action(AuthConstants.ACTION_READ).or(RundeckAccess.General.ALL_ADMIN);

        public static final String AUTH_READ_OR_OPS_ADMIN = "readOrOpsAdmin";
        /**
         * Read or ops admin
         */
        public static final AuthActions
                READ_OR_OPS_ADMIN =
                action(AuthConstants.ACTION_READ).or(RundeckAccess.General.OPS_ADMIN);

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

        final String groupName = GROUP;
        final Map<String, AuthActions> definitions = NAMED_AUTH_ACTIONS;
    }

    @Getter
    public static final class Project
            implements NamedAuthDefinition
    {
        public static final String GROUP = "project";
        public static final String TYPE = "app.project";

        public static final String AUTH_APP_CONFIGURE = "appConfigure";
        /**
         * configure or app admin
         */
        public static final AuthActions
                APP_CONFIGURE =
                action(AuthConstants.ACTION_CONFIGURE).or(RundeckAccess.General.APP_ADMIN);

        public static final String AUTH_APP_DELETE_EXECUTION = "appDeleteExecution";
        /**
         * delete execution or app admin
         */
        public static final AuthActions
                APP_DELETE_EXECUTION =
                action(AuthConstants.ACTION_DELETE_EXECUTION).or(RundeckAccess.General.APP_ADMIN);

        public static final String AUTH_APP_EXPORT = "appExport";
        /**
         * export or app admin
         */
        public static final AuthActions
                APP_EXPORT =
                action(AuthConstants.ACTION_EXPORT).or(RundeckAccess.General.APP_ADMIN);

        public static final String AUTH_APP_IMPORT = "appImport";
        /**
         * import or app admin
         */
        public static final AuthActions
                APP_IMPORT =
                action(AuthConstants.ACTION_IMPORT).or(RundeckAccess.General.APP_ADMIN);

        public static final String AUTH_APP_PROMOTE = "appPromote";
        /**
         * promote or app admin
         */
        public static final AuthActions
                APP_PROMOTE =
                action(AuthConstants.ACTION_PROMOTE).or(RundeckAccess.General.APP_ADMIN);

        public static final String AUTH_APP_SCM_EXPORT = "appScmExport";
        /**
         * scmExport, export, or appAdmin
         */
        public static final AuthActions
                APP_SCM_EXPORT =
                or(AuthConstants.ACTION_EXPORT).or(AuthConstants.ACTION_SCM_EXPORT).or(RundeckAccess.General.APP_ADMIN);

        public static final String AUTH_APP_SCM_IMPORT = "appScmImport";
        /**
         * scmExport, export, or appAdmin
         */
        public static final AuthActions
                APP_SCM_IMPORT =
                or(AuthConstants.ACTION_IMPORT).or(AuthConstants.ACTION_SCM_IMPORT).or(RundeckAccess.General.APP_ADMIN);

        public static final Map<String, AuthActions> NAMED_AUTH_ACTIONS;

        static {
            Map<String, AuthActions> named = new HashMap<String, AuthActions>() {{
                put(AUTH_APP_CONFIGURE, APP_CONFIGURE);
                put(AUTH_APP_DELETE_EXECUTION, APP_DELETE_EXECUTION);
                put(AUTH_APP_EXPORT, APP_EXPORT);
                put(AUTH_APP_IMPORT, APP_IMPORT);
                put(AUTH_APP_PROMOTE, APP_PROMOTE);
                put(AUTH_APP_SCM_EXPORT, APP_SCM_EXPORT);
                put(AUTH_APP_SCM_IMPORT, APP_SCM_IMPORT);
                putAll(General.NAMED_AUTH_ACTIONS);
            }};
            NAMED_AUTH_ACTIONS = Collections.unmodifiableMap(named);
        }

        final String groupName = GROUP;
        final Map<String, AuthActions> definitions = NAMED_AUTH_ACTIONS;
    }

    /**
     * Named auths for Types within the Application
     */
    @Getter
    public static final class ApplicationType
            implements NamedAuthDefinition
    {
        public static final String GROUP = "appType";
        public static final String TYPE = "app.resource";

        /**
         * Resource type id for a resource kind
         *
         * @param kind
         */
        public static String typeForKind(String kind) {
            return TYPE + "." + kind;
        }

        public static String kindForTypeId(String type) {
            if (type.startsWith(TYPE + ".")) {
                return type.substring(TYPE.length() + 1);
            }
            return null;
        }

        /**
         * Available resource types
         */
        public static final Set<String> RESOURCE_TYPES;
        public static final Map<String, AuthActions> NAMED_AUTH_ACTIONS;

        static {
            Map<String, AuthActions> named = new HashMap<String, AuthActions>() {{
                putAll(General.NAMED_AUTH_ACTIONS);
            }};
            NAMED_AUTH_ACTIONS = Collections.unmodifiableMap(named);

            RESOURCE_TYPES = Collections.unmodifiableSet(AuthResources.appKinds);
        }

        final String groupName = GROUP;
        final Map<String, AuthActions> definitions = NAMED_AUTH_ACTIONS;
    }

    /**
     * Named auths for Types within a Project
     */
    @Getter
    public static final class ProjectType
            implements NamedAuthDefinition
    {
        public static final String GROUP = "projectType";
        public static final String TYPE = "project.resource";

        public static String typeForKind(String kind) {
            return TYPE + "." + kind;
        }

        public static String kindForTypeId(String type) {
            if (type.startsWith(TYPE + ".")) {
                return type.substring(TYPE.length() + 1);
            }
            return null;
        }

        /**
         * Available project resource types
         */
        public static final Set<String> RESOURCE_TYPES;
        public static final Map<String, AuthActions> NAMED_AUTH_ACTIONS;

        static {
            Map<String, AuthActions> named = new HashMap<String, AuthActions>() {{
                put(General.AUTH_APP_CREATE, General.APP_CREATE);
            }};
            NAMED_AUTH_ACTIONS = Collections.unmodifiableMap(named);
            RESOURCE_TYPES = Collections.unmodifiableSet(AuthResources.projectKinds);
        }

        final String groupName = GROUP;
        final Map<String, AuthActions> definitions = NAMED_AUTH_ACTIONS;
    }

    @Getter
    public static final class Adhoc
            implements NamedAuthDefinition
    {
        public static final String GROUP = "adhoc";
        public static final String TYPE = "project.adhoc";
        public static final Map<String, AuthActions> NAMED_AUTH_ACTIONS;

        static {
            Map<String, AuthActions> named = new HashMap<String, AuthActions>() {{
                put(General.AUTH_APP_READ, General.APP_READ);
            }};
            NAMED_AUTH_ACTIONS = Collections.unmodifiableMap(named);
        }

        final String groupName = GROUP;
        final Map<String, AuthActions> definitions = NAMED_AUTH_ACTIONS;
    }
    @Getter
    public static final class ProjectAcl
            implements NamedAuthDefinition
    {
        public static final String GROUP = "projectAcl";
        public static final String TYPE = "app.projectAcl";
        public static final Map<String, AuthActions> NAMED_AUTH_ACTIONS;

        static {
            Map<String, AuthActions> named = new HashMap<String, AuthActions>() {{
                put(General.AUTH_APP_CREATE, General.APP_CREATE);
                put(General.AUTH_APP_READ, General.APP_READ);
                put(General.AUTH_APP_UPDATE, General.APP_UPDATE);
                put(General.AUTH_APP_DELETE, General.APP_DELETE);
            }};
            NAMED_AUTH_ACTIONS = Collections.unmodifiableMap(named);
        }

        final String groupName = GROUP;
        final Map<String, AuthActions> definitions = NAMED_AUTH_ACTIONS;
    }

    @Getter
    public static final class Execution
            implements NamedAuthDefinition
    {
        public static final String GROUP = "execution";
        public static final String TYPE = "project.execution";

        public static final String AUTH_APP_READ_OR_VIEW = "readOrView";
        /**
         * view, read or app admin
         */
        public static final AuthActions
                APP_READ_OR_VIEW =
                action(AuthConstants.ACTION_VIEW).or(AuthConstants.ACTION_READ).or(General.APP_ADMIN);

        public final static String AUTH_APP_KILL = "appKill";
        /**
         * kill or app admin
         */
        public final static AuthActions APP_KILL = or(AuthConstants.ACTION_KILL, General.APP_ADMIN);

        public final static String AUTH_APP_KILLAS = "appKillAs";
        /**
         * killAs or app_admin
         */
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

        final String groupName = GROUP;
        final Map<String, AuthActions> definitions = NAMED_AUTH_ACTIONS;
    }

    @Getter
    public static final class Job
            implements NamedAuthDefinition
    {
        public static final String GROUP = "job";
        public static final String TYPE = "project.job";

        public final static String AUTH_APP_READ_OR_VIEW = "appReadOrView";
        /**
         * read or view or app_admin
         */
        public final static AuthActions APP_READ_OR_VIEW = or(AuthConstants.ACTION_READ, AuthConstants.ACTION_VIEW).or(General.APP_ADMIN);

        public static final Map<String, AuthActions> NAMED_AUTH_ACTIONS;

        static {
            Map<String, AuthActions> named = new HashMap<String, AuthActions>() {{
                put(AUTH_APP_READ_OR_VIEW, APP_READ_OR_VIEW);
                putAll(General.NAMED_AUTH_ACTIONS);
            }};
            NAMED_AUTH_ACTIONS = Collections.unmodifiableMap(named);
        }

        final String groupName = GROUP;
        final Map<String, AuthActions> definitions = NAMED_AUTH_ACTIONS;
    }

}
