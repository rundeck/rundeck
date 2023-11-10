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
    default List<ComponentMeta> getMetadataForJob(String id, Set<String> names){
        return null;
    }

    /**
     * Get metadata for a job, with auth context
     *
     * @param id          job id
     * @param names       metadata names
     * @param authContext auth context
     * @return metadata list
     */
    default List<ComponentMeta> getMetadataForJob(String id, Set<String> names, UserAndRolesAuthContext authContext) {
        return getMetadataForJob(id, names);
    }

    /**
     * @return Metadata for the job
     */
    default List<ComponentMeta> getMetadataForJob(JobDataSummary job, Set<String> names){
        return null;
    }

    /**
     * @return Metadata for the job with auth context
     */
    default List<ComponentMeta> getMetadataForJob(
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
            Set<String> names,
            UserAndRolesAuthContext authContext
    )
    {
        Map<String, List<ComponentMeta>> map = new HashMap<>();
        for (String id : ids) {
            List<ComponentMeta> metadataForJob = getMetadataForJob(id, names, authContext);
            if (metadataForJob != null && !metadataForJob.isEmpty()) {
                map.put(id, metadataForJob);
            }
        }
        return map;
    }

    /**
     * @return Metadata for the jobs
     */
    default Map<String, List<ComponentMeta>> getMetadataForJobIds(
            Collection<String> ids,
            Set<String> names
    )
    {
        return getMetadataForJobIds(ids, names, null);
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
            List<ComponentMeta> metadataForJob = getMetadataForJob(job.getUuid(), names, authContext);
            if (metadataForJob != null && !metadataForJob.isEmpty()) {
                map.put(job.getUuid(), metadataForJob);
            }
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
