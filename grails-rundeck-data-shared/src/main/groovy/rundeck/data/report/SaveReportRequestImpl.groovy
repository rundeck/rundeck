package rundeck.data.report

import groovy.util.logging.Slf4j;
import org.rundeck.app.data.model.v1.report.dto.SaveReportRequest;

import java.lang.reflect.Field;

@Slf4j
class SaveReportRequestImpl implements SaveReportRequest {
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

    static void buildFromMap(SaveReportRequestImpl obj, Map<String, Object> data) {
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
                            obj.executionId = new Long((Integer)data.get(key));
                        }else if(key == "adhocExecution"){
                            obj.adhocExecution = Boolean.parseBoolean(String.valueOf(data.get(key)));
                        }else{
                            obj[key] = data.get(key)
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
    static SaveReportRequestImpl fromMap(Map data) {
        SaveReportRequestImpl SaveReportRequest = new SaveReportRequestImpl();
        buildFromMap(SaveReportRequest, data);
        return SaveReportRequest;
    }

    public static final List<String> DEPRECATED_FIELD_NAMES = Arrays.asList("ctxProject","jcJobId","jcExecId","actionType","ctxType","ctxName","ctxCommand","ctxController","maprefUri");
}