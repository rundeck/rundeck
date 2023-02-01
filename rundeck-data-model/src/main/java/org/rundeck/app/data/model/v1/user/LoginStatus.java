package org.rundeck.app.data.model.v1.user;

public enum LoginStatus {
    LOGGEDIN("LOGGED IN"),
    LOGGEDOUT("LOGGED OUT"),
    ABANDONED("ABANDONED"),
    NOTLOGGED("NOT LOGGED");
    private final String value;
    LoginStatus(String value){
        this.value = value;
    }
    public String getValue(){
        return this.value;
    }
}
