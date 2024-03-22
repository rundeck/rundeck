package org.rundeck.util.gui.common.navigation

enum NavProjectSettings {

    EXEC_MODE("Execution Mode"),
    USER_INTERFACE("User Interface")

    String tabLink

    NavProjectSettings(String tabLink) {
        this.tabLink = tabLink
    }

    String getTabLink() {
        return tabLink
    }
}