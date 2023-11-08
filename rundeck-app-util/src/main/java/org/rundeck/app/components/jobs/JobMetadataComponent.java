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
    List<JobMeta> getMetadataForJob(String id, Set<String> names);

    /**
     * Get metadata for a job, with auth context
     *
     * @param id          job id
     * @param names       metadata names
     * @param authContext auth context
     * @return metadata list
     */
    default List<JobMeta> getMetadataForJob(String id, Set<String> names, UserAndRolesAuthContext authContext) {
        return getMetadataForJob(id, names);
    }

    /**
     * @return Metadata for the job
     */
    List<JobMeta> getMetadataForJob(JobDataSummary job, Set<String> names);

    /**
     * @return Metadata for the job with auth context
     */
    default List<JobMeta> getMetadataForJob(
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
    default Map<String, List<JobMeta>> getMetadataForJobIds(
            Collection<String> ids,
            Set<String> names,
            UserAndRolesAuthContext authContext
    )
    {
        Map<String, List<JobMeta>> map = new HashMap<>();
        for (String id : ids) {
            List<JobMeta> metadataForJob = getMetadataForJob(id, names, authContext);
            if (metadataForJob != null && !metadataForJob.isEmpty()) {
                map.put(id, metadataForJob);
            }
        }
        return map;
    }

    /**
     * @return Metadata for the jobs
     */
    default Map<String, List<JobMeta>> getMetadataForJobIds(
            Collection<String> ids,
            Set<String> names
    )
    {
        return getMetadataForJobIds(ids, names, null);
    }

    /**
     * @return Metadata for the jobs
     */
    default Map<String, List<JobMeta>> getMetadataForJobs(
            Collection<JobDataSummary> jobs,
            Set<String> names,
            UserAndRolesAuthContext authContext
    )
    {
        Map<String, List<JobMeta>> map = new HashMap<>();
        for (JobDataSummary job : jobs) {
            List<JobMeta> metadataForJob = getMetadataForJob(job.getUuid(), names, authContext);
            if (metadataForJob != null && !metadataForJob.isEmpty()) {
                map.put(job.getUuid(), metadataForJob);
            }
        }
        return map;
    }

    /**
     * @return Metadata for the jobs
     */
    default Map<String, List<JobMeta>> getMetadataForJobs(Collection<JobDataSummary> jobs, Set<String> names) {
        return getMetadataForJobs(jobs, names, null);
    }

}
