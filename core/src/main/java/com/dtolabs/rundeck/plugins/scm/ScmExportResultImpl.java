package com.dtolabs.rundeck.plugins.scm;

/**
 * Created by greg on 9/4/15.
 */
public class ScmExportResultImpl implements ScmExportResult {
    private boolean success;
    private String message;
    private String extendedMessage;
    private String id;
    private ScmCommitInfo commit;

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public boolean isError() {
        return !success;
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

    @Override
    public ScmCommitInfo getCommit() {
        return commit;
    }

    public void setCommit(ScmCommitInfo commit) {
        this.commit = commit;
    }

    @Override
    public String getExtendedMessage() {
        return extendedMessage;
    }

    public void setExtendedMessage(String extendedMessage) {
        this.extendedMessage = extendedMessage;
    }
}
