package org.rundeck.app.data.model.v1.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    String actionType;
    String adhocScript;
    String tags;

    public static void buildFromMap(SaveReportRequestImpl obj, Map<String, Object> data) {
        for (String key : data.keySet()) {
            Field field;
            try {
                if(key == "ctxProject") {
                    obj.project = (String)data.get("ctxProject");
                }else if(key == "jcJobId") {
                    obj.jobId = data.get("jcJobId").toString();
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
                            field.set(obj, Boolean.parseBoolean((String)data.get(key)));
                        }else{
                            field.set(obj, data.get(key));
                        }
                    }
                }

            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }
    public static SaveReportRequestImpl fromMap(Map data) {
        SaveReportRequestImpl SaveReportRequest = new SaveReportRequestImpl();
        buildFromMap(SaveReportRequest, data);
        return SaveReportRequest;
    }

}