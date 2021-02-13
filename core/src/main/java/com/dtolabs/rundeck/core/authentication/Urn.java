package com.dtolabs.rundeck.core.authentication;

import java.io.Serializable;
import java.security.Principal;

public class Urn  implements Principal, Serializable {
    private static final long serialVersionUID = 1L;
    private final String urnName;

    public Urn(String urnName) {
        this.urnName = urnName;
    }

    @Override
    public String getName() {
        return urnName;
    }

    @Override
    public String toString() {
        return "RUNDECK urnName: " + this.urnName;
    }
}
