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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Enumeration of supported conditional operators for workflow conditional steps.
 * Each operator has a string representation used in condition definitions.
 */
public enum ConditionalOperator {
    EQUALS("=="),
    NOT_EQUALS("!="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    GREATER_THAN_OR_EQUAL(">="),
    LESS_THAN_OR_EQUAL("<="),
    CONTAINS("contains"),
    MATCHES("matches");

    private final String symbol;

    ConditionalOperator(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Get the string symbol for this operator
     * @return The operator symbol (e.g., "==", "!=", "contains", etc.)
     */
    public String getSymbol() {
        return symbol;
    }

    /**
     * Get a ConditionalOperator from its string symbol
     * @param symbol The operator symbol
     * @return The ConditionalOperator enum value, or null if not found
     */
    public static ConditionalOperator fromSymbol(String symbol) {
        if (symbol == null) {
            return null;
        }
        for (ConditionalOperator op : values()) {
            if (op.symbol.equals(symbol)) {
                return op;
            }
        }
        return null;
    }

    /**
     * Check if a string symbol is a valid operator
     * @param symbol The operator symbol to check
     * @return true if the symbol is a valid operator, false otherwise
     */
    public static boolean isValidOperator(String symbol) {
        return fromSymbol(symbol) != null;
    }

    /**
     * Get all valid operator symbols as a list
     * @return List of all operator symbols
     */
    public static List<String> getAllSymbols() {
        return Arrays.stream(values())
                .map(ConditionalOperator::getSymbol)
                .collect(Collectors.toList());
    }
}

