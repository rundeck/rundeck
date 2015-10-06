package com.dtolabs.rundeck.core.execution.utils;

import java.util.Arrays;

/**
* Created by greg on 3/19/15.
*/
public final class BasicSource implements PasswordSource{
    private byte[] password;
    public BasicSource(byte[] password) {
        this.password=password;
    }
    public BasicSource(String password) {
        this.password=null!=password?password.getBytes():null;
    }

    @Override
    public byte[] getPassword() {
        return password;
    }

    @Override
    public void clear() {
        if(password!=null) {
            Arrays.fill(password, (byte) 0);
        }
        password=null;
    }
}
