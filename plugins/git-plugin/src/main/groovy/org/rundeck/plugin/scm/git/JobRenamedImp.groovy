package org.rundeck.plugin.scm.git

import com.dtolabs.rundeck.plugins.scm.JobRenamed

class JobRenamedImp implements JobRenamed{
    String uuid
    String sourceId
    String renamedPath
}
