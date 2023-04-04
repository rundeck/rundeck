package org.rundeck.app.data.model.v1;

public interface DeletionResult {
    String getDataType();
    String getId();
    boolean getSuccess();
    String getError();
}
