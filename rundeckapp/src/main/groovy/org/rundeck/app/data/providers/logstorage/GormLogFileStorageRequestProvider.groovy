package org.rundeck.app.data.providers.logstorage

import grails.compiler.GrailsCompileStatic
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import org.hibernate.type.IntegerType
import org.hibernate.type.LongType
import org.rundeck.app.data.model.v1.logstorage.LogFileStorageRequestData
import org.rundeck.app.data.providers.v1.logstorage.LogFileStorageRequestProvider
import org.rundeck.app.data.providers.v1.logstorage.dto.CompletedStatusLogFileStorageResponse
import org.rundeck.app.data.providers.v1.logstorage.dto.DuplicateLogFileStorageResponse
import rundeck.Execution
import rundeck.LogFileStorageRequest

@GrailsCompileStatic
class GormLogFileStorageRequestProvider implements LogFileStorageRequestProvider {
    @Override
    LogFileStorageRequestData get(Long id) {
        return LogFileStorageRequest.get(id)
    }

    @Override
    LogFileStorageRequestData retryLoad(Long requestId, Long retryMaxMs) {
        LogFileStorageRequest.withNewSession {
            long start = System.currentTimeMillis()
            LogFileStorageRequest request = LogFileStorageRequest.get(requestId)
            while (!request) {
                Thread.sleep(500)
                request = LogFileStorageRequest.get(requestId)
                if ((System.currentTimeMillis() - start) > retryMaxMs) {
                    break
                }
            }
            return request
        }
    }

    @Override
    LogFileStorageRequestData build(String pluginName, String filetype, Boolean completed, String executionUuid) {
        return new LogFileStorageRequest(
                execution: findExecutionByUuid(executionUuid),
                pluginName: pluginName,
                completed: completed,
                filetype: filetype
        )
    }

    @Override
    LogFileStorageRequestData create(LogFileStorageRequestData data) throws Exception {
        var newLogFile = new LogFileStorageRequest(
                execution: findExecutionByUuid(data.executionUuid),
                pluginName: data.pluginName,
                completed: data.completed,
                filetype: data.filetype
        )
        return newLogFile.save(flush: true)
    }

    @Override
    LogFileStorageRequestData update(String executionUuid, LogFileStorageRequestData data) throws Exception {
        var currentLogFileStorage = findByExecutionUuid(executionUuid)
        currentLogFileStorage.refresh()
        currentLogFileStorage.filetype = data.filetype
        currentLogFileStorage.completed = data.completed
        currentLogFileStorage.pluginName = data.pluginName
        currentLogFileStorage.execution = Execution.get(data.executionId)
        return currentLogFileStorage.save(flush:true)
    }

    @Override
    LogFileStorageRequestData updateFiletypeAndCompleted(String executionUuid, String filetype, Boolean completed) throws Exception {
        var currentLogFileStorage = findByExecutionUuid(executionUuid)
        currentLogFileStorage.refresh()
        currentLogFileStorage.filetype = filetype
        currentLogFileStorage.completed = completed
        return currentLogFileStorage.save(flush:true)
    }

    @Override
    LogFileStorageRequestData updateCompleted(String executionUuid, Boolean completed) throws Exception {
        var currentLogFileStorage = findByExecutionUuid(executionUuid)
        currentLogFileStorage.refresh()
        currentLogFileStorage.completed = completed
        return currentLogFileStorage.save(flush:true)
    }

    @Override
    void delete(String executionUuid) {
        def execution = findExecutionByUuid(executionUuid)
        def request = findByExecution(execution)
        execution.setLogFileStorageRequest(null)
        execution.save(flush:true)
        request.delete(flush:true)
    }

    @Override
    @CompileStatic(TypeCheckingMode.SKIP)
    LogFileStorageRequestData findByExecutionId(Long executionId) {
        return LogFileStorageRequest.findByExecution(Execution.get(executionId))
    }

    @Override
    @TypeChecked(TypeCheckingMode.SKIP)
    List<DuplicateLogFileStorageResponse> findDuplicates() {
        def list = LogFileStorageRequest.createCriteria().list {
            projections{
                sqlGroupProjection 'execution_id, count(id) as dupecount', 'execution_id having count(execution_id) > 1', ['execution_id', 'dupecount'], [
                        LongType.INSTANCE, IntegerType.INSTANCE]
            }
        }.collect { new DuplicateLogFileStorageResponse(executionId: it[0] as Long, count: it[1] as Long) }
        return list
    }

    @Override
    List<CompletedStatusLogFileStorageResponse> listCompletedStatusByExecutionId(Long executionId) {
        return LogFileStorageRequest.executeQuery('select org.rundeck.app.data.providers.v1.logstorage.dto.CompletedStatusLogFileStorageResponse(id,completed) from LogFileStorageRequest where execution_id=:eid',[eid:executionId])
    }

    @Override
    @TypeChecked(TypeCheckingMode.SKIP)
    Long countByIncompleteAndClusterNodeNotInExecIds(String serverUUID, Set<Long> skipExecIds) {
        return LogFileStorageRequest.createCriteria().get{
            eq('completed',false)
            execution {
                if (null == serverUUID) {
                    isNull('serverNodeUUID')
                } else {
                    eq('serverNodeUUID', serverUUID)
                }
                if (skipExecIds) {
                    not {
                        inList('id', skipExecIds)
                    }
                }
            }
            projections{
                rowCount()
            }
        } as Long
    }

    @Override
    @TypeChecked(TypeCheckingMode.SKIP)
    List<LogFileStorageRequestData> listByIncompleteAndClusterNodeNotInExecIds(String serverUUID, Set<Long> execIds, Map paging) {
        return LogFileStorageRequest.withCriteria{
            eq('completed',false)

            execution {
                if (null == serverUUID) {
                    isNull('serverNodeUUID')
                } else {
                    eq('serverNodeUUID', serverUUID)
                }
                if (execIds) {
                    not {
                        inList('id', execIds)
                    }
                }
            }
            if(paging && paging.max){
                maxResults(paging.max.toInteger())
                firstResult(paging.offset?:0)
            }
        } as List<LogFileStorageRequestData>
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    Execution findExecutionByUuid(String uuid) {
        return Execution.findByUuid(uuid)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    LogFileStorageRequest findByExecution(Execution execution) {
        return LogFileStorageRequest.findByExecution(execution)
    }

    @CompileStatic(TypeCheckingMode.SKIP)
    LogFileStorageRequest findByExecutionUuid(String executionUuid) {
        return LogFileStorageRequest.findByExecution(Execution.findByUuid(executionUuid))
    }
}
