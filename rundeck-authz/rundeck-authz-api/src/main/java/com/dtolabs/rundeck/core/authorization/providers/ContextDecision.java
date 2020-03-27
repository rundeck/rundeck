/*
 * Copyright 2016 SimplifyOps, Inc. (http://simplifyops.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dtolabs.rundeck.core.authorization.providers;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import com.dtolabs.rundeck.core.authorization.Explanation;
import com.dtolabs.rundeck.core.authorization.Explanation.Code;

/**
 * ContextDecision provides the decision id and the evaluation up to the decision point.
 * @author noahcampbell
 *
 */
public class ContextDecision implements Explanation {
    
    private final Code id;
    private final boolean granted;
    private final List<ContextEvaluation> evaluations;
    
    /**
     * Construct a decision.
     * 
     * @param id The decision code id.
     * @param granted Is the decision granted or not.
     * @param evaluations A list of evaluations that includes the final decision.
     */
    public ContextDecision(Code id, boolean granted, List<ContextEvaluation> evaluations) {
        this.id = id;
        this.granted = granted;
        this.evaluations = evaluations;
    }
    
    public ContextDecision(Code id, boolean granted) {
        this(id, granted, new ArrayList<ContextEvaluation>());
    }
    
    public boolean granted() {
        return this.granted;
    }
    
    public Code getCode() {
        return this.id;
    }

    public void describe(PrintStream out) {
        out.println(toString());
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(granted() ? "GRANTED" : "REJECTED");
        sb.append(", reason: ");
        sb.append(id.toString());
        sb.append(", evaluations: ");
        if (null != evaluations && evaluations.size() > 0) {
            for (ContextEvaluation ce : this.evaluations) {
                sb.append("\t").append(ce.toString());
            }
        }else {
            sb.append("None");
        }
        return sb.toString();
    }

    List<ContextEvaluation> getEvaluations() {
        return evaluations;
    }
}