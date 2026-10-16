/*
 * Copyright 2026 Rundeck, Inc. (http://rundeck.com)
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

package com.dtolabs.rundeck.core.utils;

import java.util.Locale;

/**
 * Client-facing error text that does not disclose framework, SQL, or domain details.
 * Callers log the original exception and return one of these messages.
 */
public final class ClientErrorMessages {

    /** Generic failure when the request cannot be completed. */
    public static final String REQUEST_FAILED = "The request could not be completed.";

    /** Persistence failure, including SQL and Hibernate errors. */
    public static final String SAVE_FAILED = "The request could not be saved.";

    /** Bean validation or constraint failure. */
    public static final String VALIDATION_FAILED = "The request could not be saved because it is invalid.";

    /** Multipart or body larger than the configured limit. */
    public static final String REQUEST_TOO_LARGE = "The request is too large.";

    private ClientErrorMessages() {
    }

    /**
     * Maps an internal failure to a stable message.
     *
     * @param error failure to classify; may be null
     * @return a message safe to return to a client
     */
    public static String forFailure(Throwable error) {
        if (error == null) {
            return REQUEST_FAILED;
        }
        String name = error.getClass().getName().toLowerCase(Locale.ROOT);
        String message = error.getMessage() == null ? "" : error.getMessage().toLowerCase(Locale.ROOT);
        if (name.endsWith("validationexception") || name.endsWith("constraintviolationexception")) {
            return VALIDATION_FAILED;
        }
        if (isPersistenceFailure(name, message)) {
            return SAVE_FAILED;
        }
        return REQUEST_FAILED;
    }

    /**
     * Returns {@code error}'s own message when it is a short, non-internal explanation.
     * Validation, JDBC, Hibernate, and stack-like messages are replaced.
     *
     * @param error failure whose message may be shown; may be null
     * @return a message safe to return to a client
     */
    public static String publicMessage(Throwable error) {
        if (error == null) {
            return REQUEST_FAILED;
        }
        String message = error.getMessage();
        if (isInternalType(error.getClass().getName()) || looksInternal(message)) {
            return forFailure(error);
        }
        if (message == null || message.isBlank()) {
            return forFailure(error.getCause() != null ? error.getCause() : error);
        }
        return message;
    }

    private static boolean isPersistenceFailure(String className, String message) {
        return className.contains("hibernate")
                || className.contains("jdbc")
                || className.contains("sqlexception")
                || className.startsWith("org.springframework.dao.")
                || message.contains("sql");
    }

    private static boolean isInternalType(String className) {
        String name = className.toLowerCase(Locale.ROOT);
        return name.endsWith("validationexception")
                || name.endsWith("constraintviolationexception")
                || isPersistenceFailure(name, "");
    }

    private static boolean looksInternal(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return message.length() > 400
                || message.indexOf('\n') >= 0
                || lower.contains("sql")
                || message.contains("org.")
                || message.contains("java.")
                || message.contains("javax.")
                || message.contains("Caused by")
                || message.contains("\tat ");
    }
}
