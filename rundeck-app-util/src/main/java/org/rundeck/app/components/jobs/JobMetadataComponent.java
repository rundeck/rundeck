package org.rundeck.app.components.jobs;

import com.dtolabs.rundeck.core.authorization.UserAndRolesAuthContext;
import org.rundeck.app.data.model.v1.job.JobDataSummary;

import java.util.*;

/**
 * Provides metadata for jobs
 */
public interface JobMetadataComponent {
    Set<String> getAvailableMetadataNames();
//
//    /**
//     * @return Metadata for the project
//     */
//    List<JobMeta> getMetadataForProject(String project,Set<String> names);

    /**
     * @return Metadata for a job
     */
    default Optional<List<ComponentMeta>> getMetadataForJob(String id, String project, Set<String> names){
        return Optional.empty();
    }

    /**
     * Get metadata for a job, with auth context
     *
     * @param id          job id
     * @param names       metadata names
     * @param authContext auth context
     * @return metadata list
     */
    default Optional<List<ComponentMeta>> getMetadataForJob(String id, String project,Set<String> names, UserAndRolesAuthContext authContext) {
        return getMetadataForJob(id, project, names);
    }

    /**
     * @return Metadata for the job
     */
    default Optional<List<ComponentMeta>> getMetadataForJob(JobDataSummary job, Set<String> names){
        return Optional.empty();
    }

    /**
     * @return Metadata for the job with auth context
     */
    default Optional<List<ComponentMeta>> getMetadataForJob(
            JobDataSummary job,
            Set<String> names,
            UserAndRolesAuthContext authContext
    )
    {
        return getMetadataForJob(job, names);
    }

    /**
     * @return Metadata for the jobs
     */
    default Map<String, List<ComponentMeta>> getMetadataForJobIds(
            Collection<String> ids,
            String project,
            Set<String> names,
            UserAndRolesAuthContext authContext
    )
    {
        Map<String, List<ComponentMeta>> map = new HashMap<>();
        for (String id : ids) {
            Optional<List<ComponentMeta>> metadataForJob = getMetadataForJob(id, project, names, authContext);
            metadataForJob.ifPresent(metas -> map.put(id, metas));
        }
        return map;
    }

    /**
     * @return Metadata for the jobs
     */
    default Map<String, List<ComponentMeta>> getMetadataForJobIds(
            Collection<String> ids,
            String project,
            Set<String> names
    )
    {
        return getMetadataForJobIds(ids, project, names, null);
    }

    /**
     * @return Metadata for the jobs
     */
    default Map<String, List<ComponentMeta>> getMetadataForJobs(
            Collection<JobDataSummary> jobs,
            Set<String> names,
            UserAndRolesAuthContext authContext
    )
    {
        Map<String, List<ComponentMeta>> map = new HashMap<>();
        for (JobDataSummary job : jobs) {
            Optional<List<ComponentMeta>> metadataForJob = getMetadataForJob(job, names, authContext);
            metadataForJob.ifPresent(metas -> map.put(job.getUuid(), metas));
        }
        return map;
    }

    /**
     * @return Metadata for the jobs
     */
    default Map<String, List<ComponentMeta>> getMetadataForJobs(Collection<JobDataSummary> jobs, Set<String> names) {
        return getMetadataForJobs(jobs, names, null);
    }

}
