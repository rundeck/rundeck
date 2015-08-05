package com.dtolabs.rundeck.core.authorization.providers;

import com.dtolabs.rundeck.core.authorization.Explanation.Code;

public class ContextEvaluation {
    public ContextEvaluation(Code id, String command) {
        this.id = id;
        this.command = command;
    }
    public final Code id;
    public final String command;
    
    @Override
    public String toString() {
        return command + " => " + id;
    }
}