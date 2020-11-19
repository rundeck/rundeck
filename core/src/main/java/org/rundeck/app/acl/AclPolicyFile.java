package org.rundeck.app.acl;

import java.io.InputStream;
import java.util.Date;

public interface AclPolicyFile {
    InputStream getInputStream();

    Date getModified();

    Date getCreated();

    String getName();
}
