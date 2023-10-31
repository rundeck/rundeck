package org.rundeck.util.setup

enum NavBarTypes {

    KEYSTORAGE('Key Storage', '/storage')

    final String linkText
    final String url

    NavBarTypes(String linkText, String url) {
        this.linkText = linkText;
        this.url = url;
    }

    String getLinkText() {
        return linkText
    }

    String getUrl() {
        return url
    }

}