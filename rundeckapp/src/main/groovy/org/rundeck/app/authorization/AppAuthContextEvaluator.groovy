package org.rundeck.app.authorization

import com.dtolabs.rundeck.core.authorization.AuthContext
import com.dtolabs.rundeck.core.authorization.AuthContextEvaluator
import com.dtolabs.rundeck.core.common.INodeSet
import org.rundeck.app.data.model.v1.job.JobData
import rundeck.Execution

import java.util.function.Function

interface AppAuthContextEvaluator extends AuthContextEvaluator {
    /**
     * Return the resource definition for a job for use by authorization checks
     * @param se
     * @return
     */
    def Map authResourceForJob(JobData se)

    /**
     * Return true if the user is authorized to configure the project
     * @param authContext
     * @param project project name
     * @return true/false
     */
    boolean authorizeProjectConfigure(AuthContext authContext, String project)

    /**
     * Return true if the user is authorized for all actions for the execution
     * @param authContext
     * @param exec
     * @param actions
     * @return true/false
     */
    boolean authorizeProjectExecutionAll(AuthContext authContext, Execution exec, Collection<String> actions)

    /**
     * Return true if the user is authorized for any actions for the execution
     * @param authContext
     * @param exec
     * @param actions
     * @return true/false
     */
    boolean authorizeProjectExecutionAny(AuthContext authContext, Execution exec, Collection<String> actions)

    /**
     * Filter a list of Executions and return only the ones that the user has authorization for all actions in the
     * project context
     * @param framework
     * @param execs list of executions
     * @param actions
     * @return List of authorized executions
     */
    List<Execution> filterAuthorizedProjectExecutionsAll(
        AuthContext authContext,
        List<Execution> execs,
        Collection<String> actions
    )

    /**
     * Return true if the user is authorized for all actions for the job in the project context
     * @param framework
     * @param job
     * @param actions
     * @param project
     * @return true/false
     */
    boolean authorizeProjectJobAny(
        AuthContext authContext,
        JobData job,
        Collection<String> actions,
        String project
    )

    /**
     * Return true if the user is authorized for all actions for the job in the project context
     * @param framework
     * @param job
     * @param actions
     * @param project
     * @return true/false
     */
    boolean authorizeProjectJobAll(
            AuthContext authContext,
            JobData job,
            Collection<String> actions,
            String project
    )

    /**
     * @return the nodeset consisting only of the input nodes where the specified actions are all authorized
     * @param project project name
     * @param actions action set
     * @param unfiltered nodes
     * @param authContext authoriziation
     */
    public INodeSet filterAuthorizedNodes(
        final String project, final Set<String> actions, final INodeSet unfiltered,
        AuthContext authContext
    )
    public <T> Set<T> filterAuthorizedResourcesAll(
        final AuthContext authContext,
        final String project,
        final Set<String> actions,
        final Set<T> resources,
        final Function<T, Map<String, String>> convert,
        final String key
    )
}
