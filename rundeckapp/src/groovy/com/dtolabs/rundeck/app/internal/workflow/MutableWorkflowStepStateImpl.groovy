package com.dtolabs.rundeck.app.internal.workflow

import com.dtolabs.rundeck.core.execution.workflow.state.ExecutionState
import com.dtolabs.rundeck.core.execution.workflow.state.StepIdentifier
import com.dtolabs.rundeck.core.execution.workflow.state.StepState
import com.dtolabs.rundeck.core.execution.workflow.state.WorkflowState

/**
 * $INTERFACE is ...
 * User: greg
 * Date: 10/16/13
 * Time: 11:05 AM
 */
class MutableWorkflowStepStateImpl implements MutableWorkflowStepState {
    MutableStepState mutableStepState
    MutableWorkflowState mutableSubWorkflowState
    StepIdentifier stepIdentifier;
    Map<String, MutableStepState> mutableNodeStateMap;
    List<String> nodeStepTargets;
    boolean nodeStep;

    MutableWorkflowStepStateImpl(StepIdentifier stepIdentifier) {
        this(stepIdentifier,null)
    }
    MutableWorkflowStepStateImpl(StepIdentifier stepIdentifier,MutableWorkflowState subflow) {
        this.stepIdentifier = stepIdentifier
        this.mutableStepState=new MutableStepStateImpl()
        this.mutableNodeStateMap = new HashMap<String, MutableStepState>()
        this.mutableSubWorkflowState=subflow
        this.nodeStep=false
    }

    public StepState getStepState(){
        return mutableStepState
    }

    /**
     * Return a map of node name to step states for the step
     *
     * @return
     */
    public Map<String, ? extends StepState> getNodeStateMap(){
        return mutableNodeStateMap
    }

    /**
     * Return true if the step contains a sub workflow
     *
     * @return
     */
    public boolean hasSubWorkflow(){
        mutableSubWorkflowState!=null
    }

    /**
     * Return the sub workflow state
     *
     * @return
     */
    public WorkflowState getSubWorkflowState(){
        mutableSubWorkflowState
    }

    @Override
    MutableWorkflowState createMutableSubWorkflowState(Set<String> nodeSet,int count) {
        mutableSubWorkflowState = new MutableWorkflowStateImpl(nodeSet, count)
    }

    @Override
    void setNodeStepTargets(List<String> nodeset) {
        nodeStepTargets = new ArrayList<String>(nodeset)
        nodeStep=true
    }

    @Override
    public java.lang.String toString() {
        return "WFStep{" +
                "step=" + mutableStepState +
                (hasSubWorkflow()? ", sub=" + mutableSubWorkflowState :'') +
                ", id=" + stepIdentifier +
                (nodeStepTargets!=null?", targetNodes=" + nodeStepTargets :'')+
                ", nodes=" + mutableNodeStateMap +
                '}';
    }
}
