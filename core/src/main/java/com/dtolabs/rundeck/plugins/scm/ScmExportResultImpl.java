package com.dtolabs.rundeck.plugins.scm;

/**
 * Created by greg on 9/4/15.
 */
public class ScmExportResultImpl implements ScmExportResult {
    private boolean success;
    private boolean error;
    private String message;
    private String id;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
