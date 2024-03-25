package org.rundeck.util.gui.common.navigation

enum NavProjectSettings {

    EXEC_MODE("Execution Mode"),
    USER_INTERFACE("User Interface"),
    DEFAULT_FILE_COPIER("Default File Copier"),
    DEFAULT_NODE_EXECUTOR("Default Node Executor")

    String tabLink

    NavProjectSettings(String tabLink) {
        this.tabLink = tabLink
    }

    String getTabLink() {
        return tabLink
    }
}