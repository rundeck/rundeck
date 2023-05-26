package org.rundeck.app.data.model.v1.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Slf4j
public class SaveReportRequestImpl implements SaveReportRequest {
    Long executionId;
    Date dateStarted;
    String jobId;
    String reportId;
    Boolean adhocExecution;
    String succeededNodeList;
    String failedNodeList;
    String filterApplied;
    String project;
    String abortedByUser;
    String author;
    String title;
    String status;
    String node;
    String message;
    Date dateCompleted;
    String adhocScript;
    String tags;
    String jobUuid;

    public static void buildFromMap(SaveReportRequestImpl obj, Map<String, Object> data) {
        for (String key : data.keySet()) {
            Field field;
            try {
                if(key == "ctxProject") {
                    obj.project = (String)data.get("ctxProject");
                }else if(key == "jcJobId") {
                    obj.jobId = data.get("jcJobId").toString();
                }else if(key == "jobId"){
                    obj.jobId = data.get("jobId").toString();
                }else{
                    field = obj.getClass().getDeclaredField(key);
                    if(field != null){
                        if(key == "dateStarted" | key == "dateCompleted"){
                            if (!(data.get("dateCompleted") instanceof Date)) {
                                obj.dateCompleted = new Date();
                            } else {
                                obj.dateCompleted = (Date)data.get("dateCompleted");
                            }
                            if (data.get("dateStarted") instanceof Date) {
                                obj.dateStarted = (Date)data.get("dateStarted");
                            }
                        }else if(key == "executionId"){
                            field.set(obj, new Long((Integer)data.get(key)));
                        }else if(key == "adhocExecution"){
                            field.set(obj, Boolean.parseBoolean(String.valueOf(data.get(key))));
                        }else{
                            field.set(obj, data.get(key));
                        }
                    }
                }

            } catch (NoSuchFieldException nsfe) {
                if(!DEPRECATED_FIELD_NAMES.contains(key)) {
                    log.info("Report builder found unknown field: " + key);
                }
            } catch (IllegalAccessException iex) {
                log.warn("Unable to set field: "+key+" illegal access");
            }
        }
    }
    public static SaveReportRequestImpl fromMap(Map data) {
        SaveReportRequestImpl SaveReportRequest = new SaveReportRequestImpl();
        buildFromMap(SaveReportRequest, data);
        return SaveReportRequest;
    }

    public static final List<String> DEPRECATED_FIELD_NAMES = Arrays.asList("ctxProject","jcJobId","jcExecId","actionType","ctxType","ctxName","ctxCommand","ctxController","maprefUri");

}