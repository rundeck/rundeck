package com.dtolabs.rundeck.plugins.scm;

import java.io.OutputStream;

/**
 * Created by greg on 8/21/15.
 */
public interface JobSerializer {

    void serialize(String format, OutputStream outputStream);
}
