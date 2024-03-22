package org.rundeck.util.gui.common.navigation

enum NavLinkTypes {

    DASHBOARD ("nav-project-dashboard-link", "/home", false),
    JOBS ("nav-jobs-link", "/jobs", false),
    NODES ("nav-nodes-link", "/nodes", false),
    COMMANDS ("nav-commands-link", "/command/run", false),
    ACTIVITY ("nav-activity-link", "/activity", false),
    HEALTH_CHECK("nav-health-checks-link","/healthcheck", false),
    CALENDARS("nav-calendars-link","/calendars", false),
    WEBHOOKS("nav-webhooks-link","/webhook", false),
    EDIT_NODES("nav-project-settings-edit-nodes","/nodes/sources", true),
    MOTD("nav-project-settings-edit-motd" , "filename=motd.md", true),
    README("nav-project-settings-edit-readme", "filename=readme.md", true),
    PROJECT_CONFIG("nav-project-settings-edit-project", "/configure", true)

    private String id
    private String url
    private boolean projectConfig

    NavLinkTypes(String id, String url, boolean projectConfig) {
        this.id = id;
        this.url = url;
        this.projectConfig = projectConfig;
    }

    String getId() {
        return id
    }

    String getUrl() {
        return url
    }

    boolean getProjectConfig() {
        return projectConfig
    }
}