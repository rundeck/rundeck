package com.dtolabs.rundeck.core.execution.workflow;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Listens to step and node context changes, and maintains thread-local step+node context
 */
public class StepContextWorkflowExecutionListener<NODE, STEP> implements StepNodeContextListener<NODE, STEP>,
        StepNodeContext<NODE, STEP> {

    /**
     * Thread local context stack, inherited by sub threads.
     */
    private InheritableThreadLocal<STEP> localStep = new InheritableThreadLocal<STEP>();
    private InheritableThreadLocal<NODE> localNode = new InheritableThreadLocal<NODE>();
    private InheritableThreadLocal<ContextStack<STEP>> contextStack = new
            InheritableThreadLocal<ContextStack<STEP>>();

    public void beginContext() {
        STEP info = localStep.get();
        if (null != info) {
            //within another workflow already, so push context onto stack
            if (null != contextStack.get()) {
                contextStack.set(contextStack.get().copyPush(info));
            } else {
                contextStack.set(ContextStack.create(info));
            }
        }
        localStep.set(null);
        localNode.set(null);
    }

    public void finishContext() {
        ContextStack<STEP> stack = contextStack.get();
        if (null != stack) {
            //pop any workflow context already on stack
            if (stack.size() > 0) {
                localStep.set(stack.pop());
            } else {
                localStep.set(null);
                contextStack.set(null);
            }
        }else{
            localStep.set(null);
        }
        localNode.set(null);
    }

    public void beginStepContext(STEP step) {
        localStep.set(step);
    }

    public void finishStepContext() {
        localStep.set(null);
    }

    public void beginNodeContext(NODE node) {
        localNode.set(node);
    }

    public void finishNodeContext() {
        localNode.set(null);
    }


    public NODE getCurrentNode() {
        return localNode.get();
    }

    public List<STEP> getCurrentContext() {
        STEP step = localStep.get();
        if (null != step) {
            if (null != contextStack.get()) {
                return contextStack.get().copyPush(step).stack();
            } else {
                return ContextStack.create(step).stack();
            }
        } else if (null != contextStack.get()) {
            List<STEP> stack = contextStack.get().stack();
            if (stack.size() > 0) {
                return stack;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
