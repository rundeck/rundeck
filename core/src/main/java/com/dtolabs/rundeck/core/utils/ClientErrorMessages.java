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

    /** How many causes to inspect before stopping, including a cause cycle. */
    private static final int CAUSE_LIMIT = 8;

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
        if (matchesValidation(error)) {
            return VALIDATION_FAILED;
        }
        if (matchesPersistence(error)) {
            return SAVE_FAILED;
        }
        return REQUEST_FAILED;
    }

    /**
     * Returns {@code error}'s own message when it is a short, non-internal explanation.
     * Validation, JDBC, Hibernate, filesystem paths, package names, and stack-like messages are replaced.
     * Short intentional text, such as "Schedule not found", is kept.
     *
     * @param error failure whose message may be shown; may be null
     * @return a message safe to return to a client
     */
    public static String publicMessage(Throwable error) {
        if (error == null) {
            return REQUEST_FAILED;
        }
        String message = error.getMessage();
        if (isInternalType(error) || looksInternal(message)) {
            return forFailure(error);
        }
        if (message == null || message.isBlank()) {
            return forFailure(error.getCause() != null ? error.getCause() : error);
        }
        return message;
    }

    private static boolean matchesValidation(Throwable error) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < CAUSE_LIMIT; depth++) {
            if (isValidationClass(current.getClass().getName().toLowerCase(Locale.ROOT))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isValidationClass(String className) {
        return className.endsWith("validationexception") || className.endsWith("constraintviolationexception");
    }

    private static boolean matchesPersistence(Throwable error) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < CAUSE_LIMIT; depth++) {
            String name = current.getClass().getName().toLowerCase(Locale.ROOT);
            String message = current.getMessage() == null ? "" : current.getMessage().toLowerCase(Locale.ROOT);
            if (isPersistenceClass(current.getClass(), name) || message.contains("sql")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isPersistenceClass(Class<?> type, String className) {
        return className.contains("hibernate")
                || className.contains("jdbc")
                || className.startsWith("org.springframework.dao.")
                || java.sql.SQLException.class.isAssignableFrom(type);
    }

    private static boolean isInternalType(Throwable error) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < CAUSE_LIMIT; depth++) {
            String name = current.getClass().getName().toLowerCase(Locale.ROOT);
            if (isValidationClass(name) || isPersistenceClass(current.getClass(), name)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean looksInternal(String message) {
        if (message == null) {
            return false;
        }
        if (message.length() > 400
                || message.indexOf('\n') >= 0
                || message.indexOf('/') >= 0
                || message.indexOf('\\') >= 0) {
            return true;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("sql")
                || message.contains("org.")
                || message.contains("java.")
                || message.contains("javax.")
                || message.contains("jakarta.")
                || message.contains("com.")
                || message.contains("groovy.")
                || message.contains("Caused by")
                || message.contains("\tat ");
    }
}
