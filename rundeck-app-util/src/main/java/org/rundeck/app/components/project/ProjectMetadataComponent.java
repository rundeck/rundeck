package org.rundeck.app.components.project;

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import org.rundeck.app.components.jobs.ComponentMeta;

import java.util.List;
import java.util.Set;

public interface ProjectMetadataComponent {
    Set<String> getAvailableMetadataNames();

    /**
     * @return Metadata for a job
     */
    List<ComponentMeta> getMetadataForProject(String project, Set<String> names, UserAndRolesAuthContext authContext);
}
