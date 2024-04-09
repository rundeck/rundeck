package org.rundeck.app.data.providers.logstorage

import grails.compiler.GrailsCompileStatic
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import org.rundeck.app.data.model.v1.logstorage.LogFileStorageRequestData
import org.rundeck.app.data.providers.v1.logstorage.LogFileStorageRequestProvider
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
        var currentLogFileStorage = findLogFileStorageRequestByExecutionUuid(executionUuid)
        currentLogFileStorage.refresh()
        currentLogFileStorage.filetype = data.filetype
        currentLogFileStorage.completed = data.completed
        currentLogFileStorage.pluginName = data.pluginName
        currentLogFileStorage.execution = Execution.get(data.executionId)
        return currentLogFileStorage.save(flush:true)
    }

    @Override
    LogFileStorageRequestData updateFiletypeAndCompleted(String executionUuid, String filetype, Boolean completed) throws Exception {
        var currentLogFileStorage = findLogFileStorageRequestByExecutionUuid(executionUuid)
        currentLogFileStorage.refresh()
        currentLogFileStorage.filetype = filetype
        currentLogFileStorage.completed = completed
        return currentLogFileStorage.save(flush:true)
    }

    @Override
    LogFileStorageRequestData updateCompleted(String executionUuid, Boolean completed) throws Exception {
        var currentLogFileStorage = findLogFileStorageRequestByExecutionUuid(executionUuid)
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
    LogFileStorageRequestData findByExecutionUuid(String executionUuid) {
        return LogFileStorageRequest.findByExecution(Execution.findByUuid(executionUuid))
    }

    @Override
    @TypeChecked(TypeCheckingMode.SKIP)
    Long countByIncompleteAndClusterNodeNotInExecUuids(String serverUUID, Set<String> skipExecUuids) {
        return LogFileStorageRequest.createCriteria().get{
            eq('completed',false)
            execution {
                if (null == serverUUID) {
                    isNull('serverNodeUUID')
                } else {
                    eq('serverNodeUUID', serverUUID)
                }
                if (skipExecUuids) {
                    not {
                        inList('uuid', skipExecUuids)
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
    List<LogFileStorageRequestData> listByIncompleteAndClusterNodeNotInExecUuids(String serverUUID, Set<String> execUuids, Map paging) {
        return LogFileStorageRequest.withCriteria{
            eq('completed',false)

            execution {
                if (null == serverUUID) {
                    isNull('serverNodeUUID')
                } else {
                    eq('serverNodeUUID', serverUUID)
                }
                if (execUuids) {
                    not {
                        inList('uuid', execUuids)
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
    LogFileStorageRequest findLogFileStorageRequestByExecutionUuid(String uuid) {
        return LogFileStorageRequest.findByExecution(Execution.findByUuid(uuid))
    }
}
