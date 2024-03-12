package org.rundeck.util.gui.common.navigation

enum NavProjectSettings {

    EXEC_MODE("tab_category_executionMode"),
    USER_INTERFACE("tab_category_gui")

    String tabLink

    NavProjectSettings(String tabLink) {
        this.tabLink = tabLink
    }

    String getTabLink() {
        return tabLink
    }
}