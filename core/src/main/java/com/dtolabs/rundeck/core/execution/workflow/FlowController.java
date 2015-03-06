package com.dtolabs.rundeck.core.execution.workflow;

/**
 * record flow control
 */
public class FlowController implements FlowControl, WorkflowStatusResult {
    private ControlBehavior controlBehavior=ControlBehavior.Continue;
    private String statusString=null;
    private boolean success=false;
    private boolean controlled=false;


    @Override
    public void Halt(final String statusString) {
        controlBehavior = ControlBehavior.Halt;
        this.statusString = statusString;
        this.controlled=true;

    }

    @Override
    public void Halt(final boolean success) {
        controlBehavior = ControlBehavior.Halt;
        this.success = success;
        this.controlled=true;
    }

    @Override
    public void Continue() {
        controlBehavior = ControlBehavior.Continue;
        this.controlled = true;
    }

    @Override
    public String getStatusString() {
        return statusString;
    }
    public boolean isCustomStatusString() {
        return null != statusString &&
               !FlowControl.STATUS_FAILED.equalsIgnoreCase(statusString) &&
               !FlowControl.STATUS_SUCCEEDED.equalsIgnoreCase(statusString);
    }

    public ControlBehavior getControlBehavior() {
        return controlBehavior;
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    /**
     * @return true if Halt or Continue has been called.
     */
    public boolean isControlled() {
        return controlled;
    }
}
