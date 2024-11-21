package org.rundeck.core.projects;

import com.dtolabs.rundeck.core.authorization.AuthContext;
import com.dtolabs.rundeck.core.common.IFramework;
import com.dtolabs.rundeck.core.common.IRundeckProject;

import java.io.OutputStream;
import java.util.Map;

public interface ProjectArchiver {


    void exportProjectArchiveToOutputStream(IRundeckProject project,
                                            IFramework framework,
                                            OutputStream stream,
                                            Map<String, Object> options,
                                            AuthContext authContext);


}
