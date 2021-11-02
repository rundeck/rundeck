package org.rundeck.core.auth;

import java.util.*;

public class AuthResources {

    public static final Set<String> projectTypes = new HashSet<>(
            Arrays.asList(
                    AuthConstants.TYPE_ADHOC,
                    AuthConstants.TYPE_JOB,
                    AuthConstants.TYPE_NODE,
                    AuthConstants.TYPE_STORAGE
            )
    );
    public static final Set<String> projectKinds = new HashSet<>(
            Arrays.asList(
                    AuthConstants.TYPE_JOB,
                    AuthConstants.TYPE_NODE,
                    AuthConstants.TYPE_EVENT,
                    AuthConstants.TYPE_WEBHOOK
            )
    );
    public static final Set<String> appTypes = new HashSet<>(
            Arrays.asList(
                    AuthConstants.TYPE_PROJECT,
                    AuthConstants.TYPE_PROJECT_ACL,
                    AuthConstants.TYPE_STORAGE,
                    AuthConstants.TYPE_APITOKEN
            )
    );
    public static final Set<String> appKinds = new HashSet<>(
            Arrays.asList(
                    AuthConstants.TYPE_PROJECT,
                    AuthConstants.TYPE_SYSTEM,
                    AuthConstants.TYPE_SYSTEM_ACL,
                    AuthConstants.TYPE_USER,
                    AuthConstants.TYPE_JOB,
                    AuthConstants.TYPE_APITOKEN,
                    AuthConstants.TYPE_PLUGIN,
                    AuthConstants.TYPE_WEBHOOK
            )
    );

    public static final List<String> appProjectActions =
            Arrays.asList(
                    AuthConstants.ACTION_ADMIN,
                    AuthConstants.ACTION_APP_ADMIN,
                    AuthConstants.ACTION_READ,
                    AuthConstants.ACTION_CONFIGURE,
                    AuthConstants.ACTION_DELETE,
                    AuthConstants.ACTION_IMPORT,
                    AuthConstants.ACTION_EXPORT,
                    AuthConstants.ACTION_DELETE_EXECUTION,
                    AuthConstants.ACTION_SCM_IMPORT,
                    AuthConstants.ACTION_SCM_EXPORT
            );
    public static final List<String> appProjectAclActions =
            Arrays.asList(
                    AuthConstants.ACTION_READ,
                    AuthConstants.ACTION_CREATE,
                    AuthConstants.ACTION_UPDATE,
                    AuthConstants.ACTION_DELETE,
                    AuthConstants.ACTION_ADMIN,
                    AuthConstants.ACTION_APP_ADMIN
            );
    public static final List<String> storageActions =
            Arrays.asList(
                    AuthConstants.ACTION_CREATE,
                    AuthConstants.ACTION_READ,
                    AuthConstants.ACTION_UPDATE,
                    AuthConstants.ACTION_DELETE
            );
    public static final List<String> appApitokenActions =
            Collections.singletonList(
                    AuthConstants.ACTION_CREATE
            );
    public static final List<String> appProjectKindActions =
            Collections.singletonList(
                    AuthConstants.ACTION_CREATE
            );
    public static final List<String> appSystemKindActions =
            Arrays.asList(
                    AuthConstants.ACTION_READ,
                    AuthConstants.ACTION_ENABLE_EXECUTIONS,
                    AuthConstants.ACTION_DISABLE_EXECUTIONS,
                    AuthConstants.ACTION_ADMIN,
                    AuthConstants.ACTION_APP_ADMIN,
                    AuthConstants.ACTION_OPS_ADMIN
            );
    public static final List<String> appSystemAclKindActions =
            Arrays.asList(
                    AuthConstants.ACTION_READ,
                    AuthConstants.ACTION_CREATE,
                    AuthConstants.ACTION_UPDATE,
                    AuthConstants.ACTION_DELETE,
                    AuthConstants.ACTION_ADMIN,
                    AuthConstants.ACTION_APP_ADMIN
            );
    public static final List<String> appUserKindActions =
            Arrays.asList(
                    AuthConstants.ACTION_ADMIN,
                    AuthConstants.ACTION_APP_ADMIN
            );
    public static final List<String> appJobKindActions =
            Arrays.asList(
                    AuthConstants.ACTION_ADMIN,
                    AuthConstants.ACTION_APP_ADMIN,
                    AuthConstants.ACTION_OPS_ADMIN
            );
    public static final List<String> appApitokenKindActions =
            Arrays.asList(
                    AuthConstants.ACTION_ADMIN,
                    AuthConstants.ACTION_APP_ADMIN,
                    AuthConstants.ACTION_GENERATE_USER_TOKEN,
                    AuthConstants.ACTION_GENERATE_SERVICE_TOKEN
            );
    public static final Map<String, List<String>> appResActionsByType;
    public static final Map<String, List<String>> appResAttrsByType;

    public static final List<String> appPluginActions = Arrays.asList(
            AuthConstants.ACTION_READ,
            AuthConstants.ACTION_INSTALL,
            AuthConstants.ACTION_UNINSTALL,
            AuthConstants.ACTION_ADMIN,
            AuthConstants.ACTION_APP_ADMIN,
            AuthConstants.ACTION_OPS_ADMIN
    );
    public static final List<String> appWebhookKindActions = Arrays.asList(
            AuthConstants.ACTION_READ,
            AuthConstants.ACTION_CREATE,
            AuthConstants.ACTION_UPDATE,
            AuthConstants.ACTION_DELETE,
            AuthConstants.ACTION_POST,
            AuthConstants.ACTION_ADMIN,
            AuthConstants.ACTION_APP_ADMIN
    );

    static {
        appResActionsByType = new HashMap<>();
        appResActionsByType.put(AuthConstants.TYPE_PROJECT, appProjectActions);
        appResActionsByType.put(AuthConstants.TYPE_PROJECT_ACL, appProjectAclActions);
        appResActionsByType.put(AuthConstants.TYPE_STORAGE, storageActions);
        appResActionsByType.put(AuthConstants.TYPE_APITOKEN, appApitokenActions);
    }

    static {
        appResAttrsByType = new HashMap<>();
        appResAttrsByType.put(AuthConstants.TYPE_PROJECT, Collections.singletonList("name"));
        appResAttrsByType.put(AuthConstants.TYPE_PROJECT_ACL, Collections.singletonList("name"));
        appResAttrsByType.put(AuthConstants.TYPE_STORAGE, Arrays.asList("path", "name"));
        appResAttrsByType.put(AuthConstants.TYPE_APITOKEN, Arrays.asList("username", "roles"));

    }

    public static final Map<String, List<String>> appKindActionsByType;

    static {

        appKindActionsByType = new HashMap<>();
        appKindActionsByType.put(AuthConstants.TYPE_PROJECT, appProjectKindActions);
        appKindActionsByType.put(AuthConstants.TYPE_SYSTEM, appSystemKindActions);
        appKindActionsByType.put(AuthConstants.TYPE_SYSTEM_ACL, appSystemAclKindActions);
        appKindActionsByType.put(AuthConstants.TYPE_USER, appUserKindActions);
        appKindActionsByType.put(AuthConstants.TYPE_JOB, appJobKindActions);
        appKindActionsByType.put(AuthConstants.TYPE_APITOKEN, appApitokenKindActions);
        appKindActionsByType.put(AuthConstants.TYPE_PLUGIN, appPluginActions);
        appKindActionsByType.put(AuthConstants.TYPE_WEBHOOK, appWebhookKindActions);
    }


    public static final List<String> projectJobActions =
            Arrays.asList(
                    AuthConstants.ACTION_READ,
                    AuthConstants.ACTION_VIEW,
                    AuthConstants.ACTION_UPDATE,
                    AuthConstants.ACTION_DELETE,
                    AuthConstants.ACTION_RUN,
                    AuthConstants.ACTION_RUNAS,
                    AuthConstants.ACTION_KILL,
                    AuthConstants.ACTION_KILLAS,
                    AuthConstants.ACTION_CREATE,
                    AuthConstants.ACTION_TOGGLE_EXECUTION,
                    AuthConstants.ACTION_TOGGLE_SCHEDULE,
                    AuthConstants.ACTION_SCM_UPDATE,
                    AuthConstants.ACTION_SCM_CREATE,
                    AuthConstants.ACTION_SCM_DELETE,
                    AuthConstants.VIEW_HISTORY
            );
    public static final List<String> projectJobKindActions =
            Arrays.asList(
                    AuthConstants.ACTION_CREATE,
                    AuthConstants.ACTION_DELETE
            );
    public static final List<String> projectAdhocActions =
            Arrays.asList(
                    AuthConstants.ACTION_READ,
                    AuthConstants.ACTION_VIEW,
                    AuthConstants.ACTION_RUN,
                    AuthConstants.ACTION_RUNAS,
                    AuthConstants.ACTION_KILL,
                    AuthConstants.ACTION_KILLAS
            );
    public static final List<String> projectNodeActions =
            Arrays.asList(
                    AuthConstants.ACTION_READ,
                    AuthConstants.ACTION_RUN
            );
    public static final Map<String, List<String>> projResActionsByType;
    public static final Map<String, List<String>> projResAttrsByType;

    static {
        projResActionsByType = new HashMap<>();
        projResActionsByType.put(AuthConstants.TYPE_JOB, projectJobActions);
        projResActionsByType.put(AuthConstants.TYPE_ADHOC, projectAdhocActions);
        projResActionsByType.put(AuthConstants.TYPE_NODE, projectNodeActions);
        projResActionsByType.put(AuthConstants.TYPE_STORAGE, storageActions);
    }


    static {
        projResAttrsByType = new HashMap<>();
        projResAttrsByType.put(AuthConstants.TYPE_JOB, Arrays.asList("group", "name", "uuid"));
        projResAttrsByType.put(AuthConstants.TYPE_ADHOC, new ArrayList<String>());
        List<String> nodeAttributeNames = Arrays.asList(
                "nodename",
                "rundeck_server",
                "username",
                "hostname",
                "osFamily",
                "osVersion",
                "(etc. any node attribute)"
        );
        projResAttrsByType.put(AuthConstants.TYPE_NODE, nodeAttributeNames);
        projResAttrsByType.put(AuthConstants.TYPE_STORAGE, Arrays.asList("path", "name"));
    }

    public static final List<String> projectNodeKindActions =
            Arrays.asList(
                    AuthConstants.ACTION_READ,
                    AuthConstants.ACTION_CREATE,
                    AuthConstants.ACTION_UPDATE,
                    AuthConstants.ACTION_REFRESH
            );
    public static final List<String> projectEventKindActions =
            Arrays.asList(
                    AuthConstants.ACTION_READ,
                    AuthConstants.ACTION_CREATE
            );
    public static final Map<String, List<String>> projKindActionsByType;
    static final List<String> projectWebhookKindActions = Arrays.asList(AuthConstants.ACTION_READ,
                                                                        AuthConstants.ACTION_CREATE,
                                                                        AuthConstants.ACTION_UPDATE,
                                                                        AuthConstants.ACTION_DELETE,
                                                                        AuthConstants.ACTION_ADMIN,
                                                                        AuthConstants.ACTION_APP_ADMIN,
                                                                        AuthConstants.ACTION_POST);

    static {

        projKindActionsByType = new HashMap<>();
        projKindActionsByType.put(AuthConstants.TYPE_JOB, projectJobKindActions);
        projKindActionsByType.put(AuthConstants.TYPE_NODE, projectNodeKindActions);
        projKindActionsByType.put(AuthConstants.TYPE_EVENT, projectEventKindActions);
        projKindActionsByType.put(AuthConstants.TYPE_WEBHOOK, projectWebhookKindActions);
        projKindActionsByType.put(AuthConstants.TYPE_STORAGE, storageActions);
    }
}
