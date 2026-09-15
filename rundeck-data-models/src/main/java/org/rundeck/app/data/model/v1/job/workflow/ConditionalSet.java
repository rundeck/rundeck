/*
 * Copyright 2024 Rundeck, Inc. (http://rundeck.com)
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
package org.rundeck.app.data.model.v1.job.workflow;

import java.util.List;
import java.util.Map;

/**
 * Represents a set of conditions organized in AND/OR groups.
 * The structure uses nested lists where, by default (isInvertLogic() == false):
 * - Each inner list represents an AND group (all conditions must be true)
 * - The outer list represents OR groups (any group being true passes the ConditionalSet)
 *
 * Example:
 * conditionGroups = [
 *   [Condition1, Condition2],  // AND group 1: Condition1 AND Condition2
 *   [Condition3],              // AND group 2: Condition3
 *   [Condition4, Condition5]   // AND group 3: Condition4 AND Condition5
 * ]
 * Evaluates as: (Condition1 AND Condition2) OR Condition3 OR (Condition4 AND Condition5)
 *
 * When isInvertLogic() is true, the polarity is inverted: each inner list becomes an
 * OR group (any condition in the group must be true), and the outer list becomes an
 * AND group (every group must pass). The same example above would instead evaluate as:
 * (Condition1 OR Condition2) AND Condition3 AND (Condition4 OR Condition5)
 */
public interface ConditionalSet {
    /**
     * Returns a list of condition groups.
     * Each inner list represents an AND group (all conditions must be true).
     * The outer list represents OR groups (any group being true passes).
     * @return List of condition groups, where each group is a list of ConditionalDefinitions
     */
    List<List<ConditionalDefinition>> getConditionGroups();

    boolean isNodeStep();

    /**
     * Whether the AND/OR polarity of {@link #getConditionGroups()} is inverted:
     * conditions within a group are OR'd together, and groups are AND'd together.
     * Defaults to false, preserving the original AND-within/OR-across semantics.
     * @return true if inverted (OR-within-group / AND-across-groups)
     */
    boolean isInvertLogic();

    Map toMap();

    ConditionalSet fromMap(Map<String, Object> map);
}

