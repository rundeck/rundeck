/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
