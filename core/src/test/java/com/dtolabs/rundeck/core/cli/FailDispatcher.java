package com.dtolabs.rundeck.core.cli;

import com.dtolabs.rundeck.core.common.INodeSet;
import com.dtolabs.rundeck.core.dispatcher.*;

import java.io.OutputStream;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import static junit.framework.Assert.fail;

/**
 * Created by greg on 2/24/15.
 */
public class FailDispatcher implements CentralDispatcher {

    public QueuedItemResult queueDispatcherJob(IDispatchedJob job) throws CentralDispatcherException {
        fail("unexpected call to queueDispatcherJob");
        return null;
    }

    public QueuedItemResult queueDispatcherScript(final IDispatchedScript dispatch) throws
            CentralDispatcherException {
        fail("unexpected call to queueDispatcherScript");
        return null;
    }

    public Collection<QueuedItem> listDispatcherQueue(final String project) throws CentralDispatcherException {
        //
        fail("unexpected call to listDispatcherQueue");
        return null;
    }
    public Collection<QueuedItem> listDispatcherQueue() throws CentralDispatcherException {
        //
        fail("unexpected call to listDispatcherQueue");
        return null;
    }

    @Override
    public PagedResult<QueuedItem> listDispatcherQueue(final String project, final Paging paging)
            throws CentralDispatcherException
    {
        fail("unexpected call to listDispatcherQueue");
        return null;
    }

    public DispatcherResult killDispatcherExecution(final String id) throws CentralDispatcherException {
        fail("unexpected call to killDispatcherExecution");
        return null;
    }

    public ExecutionFollowResult followDispatcherExecution(String id, ExecutionFollowRequest request,
                                                           ExecutionFollowReceiver receiver) throws
            CentralDispatcherException {
        fail("unexpected call to followDispatcherExecution");
        return null;
    }

    public Collection<IStoredJobLoadResult> loadJobs(ILoadJobsRequest request, java.io.File input, JobDefinitionFileFormat format) throws
            CentralDispatcherException {
        fail("unexpected call to loadJobs");
        return null;
    }

    public Collection<IStoredJob> listStoredJobs(IStoredJobsQuery query, OutputStream output,
                                                 JobDefinitionFileFormat format) throws
            CentralDispatcherException {
        fail("unexpected call to listStoredJobs");
        return null;
    }

    public void reportExecutionStatus(String project, String title, String status, int totalNodeCount,
                                      int successNodeCount, String tags, String script, String summary,
                                      Date start,
                                      Date end) throws CentralDispatcherException {
        fail("unexpected call to reportExecutionStatus");
    }

    public Collection<DeleteJobResult> deleteStoredJobs(Collection<String> jobIds) throws CentralDispatcherException {
        fail("unexpected call to deleteStoredJobs");
        return null;
    }

    public ExecutionDetail getExecution(String execId) throws CentralDispatcherException {
        fail("unexpected call to getExecution");
        return null;
    }
    @Override
    public void createProject(final String project, final Properties projectProperties)
            throws CentralDispatcherException
    {

        fail("unexpected call to createProject");
    }
    @Override
    public INodeSet filterProjectNodes(final String project, final String filter)
            throws CentralDispatcherException
    {
        fail("unexpected call to filterProjectNodes");
        return null;
    }

    @Override
    public List<String> listProjectNames() throws CentralDispatcherException {
        fail("unexpected call to listProjectNames");
        return null;
    }
}
