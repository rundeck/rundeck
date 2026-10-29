/*
 * Copyright 2026 PagerDuty, Inc.
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

package com.dtolabs.rundeck.core.resources;

import com.dtolabs.rundeck.core.plugins.configuration.ConfigurationException;

import java.io.File;
import java.io.IOException;

/**
 * Validates file resource-model paths against a project directory and optional
 * administrator-configured base paths.
 */
public final class FileResourceModelPathPolicy {
    public static final String ALLOWED_BASE_PATHS_CONFIG_KEY =
            "resourceModelSource.file.allowedBasePaths";

    private final File projectDirectory;
    private final String allowedBasePaths;

    /**
     * Creates a file resource-model path policy.
     *
     * @param projectDirectory owning project directory
     * @param allowedBasePaths comma-separated additional base paths, or {@code null}
     */
    public FileResourceModelPathPolicy(final File projectDirectory, final String allowedBasePaths) {
        this.projectDirectory = projectDirectory;
        this.allowedBasePaths = allowedBasePaths;
    }

    /**
     * Creates a file resource-model path policy from a projects directory and project name.
     *
     * @param projectsDirectory framework projects directory
     * @param project project name
     * @param allowedBasePaths comma-separated additional base paths, or {@code null}
     */
    public FileResourceModelPathPolicy(
            final File projectsDirectory,
            final String project,
            final String allowedBasePaths
    ) {
        this(new File(projectsDirectory, project), allowedBasePaths);
    }

    /**
     * Validates a configured file using canonical paths to prevent traversal.
     *
     * @param configuredFile configured resource-model file
     * @throws ConfigurationException if the path is outside the permitted directories
     */
    public void validate(final File configuredFile) throws ConfigurationException {
        if (configuredFile == null) {
            return;
        }

        try {
            File canonicalFile = configuredFile.getCanonicalFile();
            File canonicalProjectDirectory = projectDirectory.getCanonicalFile();
            if (isPathUnder(canonicalFile, canonicalProjectDirectory)) {
                return;
            }
            validateAllowedBasePaths(canonicalFile);
        } catch (IOException e) {
            throw new ConfigurationException("Invalid file path");
        }
    }

    /**
     * Validates a canonical file against the configured additional base paths.
     */
    private void validateAllowedBasePaths(final File canonicalFile)
            throws IOException, ConfigurationException {
        if (allowedBasePaths == null || allowedBasePaths.trim().isEmpty()) {
            throw new ConfigurationException("File path must be within the project directory");
        }

        for (String path : allowedBasePaths.split(",")) {
            String trimmedPath = path.trim();
            if (!trimmedPath.isEmpty()
                    && isPathUnder(canonicalFile, new File(trimmedPath).getCanonicalFile())) {
                return;
            }
        }

        throw new ConfigurationException(
                "File path is not within project directory or allowed base paths"
        );
    }

    /**
     * Returns whether the file is the base directory or one of its descendants.
     */
    private static boolean isPathUnder(final File file, final File baseDirectory) {
        return file.toPath().startsWith(baseDirectory.toPath());
    }
}
