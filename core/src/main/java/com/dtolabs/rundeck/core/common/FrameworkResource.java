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

package com.dtolabs.rundeck.core.common;

import com.dtolabs.rundeck.core.utils.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a generic framework resource. Each resoure has a name, a
 * base directory and a set of properties.
 * <br>
 */
public class FrameworkResource implements IFrameworkResource {
    public static final String VALID_RESOURCE_NAME_CHARSET_REGEX = "[-_a-zA-Z0-9+][-\\._a-zA-Z0-9+]*";
    public static final String VALID_RESOURCE_NAME_REGEX = "^"+VALID_RESOURCE_NAME_CHARSET_REGEX+"$";
    public static final String VALID_RESOURCE_DESCRIPTION_CHARSET_REGEX = "[a-zA-Z0-9\\p{L}\\p{M}\\s\\.,\\(\\)_-]+";
    public static final String VALID_RESOURCE_DESCRIPTION_REGEX = "^"+VALID_RESOURCE_DESCRIPTION_CHARSET_REGEX+"$";

    final Logger logger;

    /**
     * Constructor
     *
     * @param name Name of resource
     * @param dir  Base directory of resource
     */
    public FrameworkResource(final String name, final File dir) {
        if ("".equals(name)) {
            throw new IllegalArgumentException("empty string cannot be used as a name");
        }
        this.name = name;
        baseDir = dir;
        logger = Logger.getLogger(this.getClass().getName());
    }

    protected Logger getLogger() {
        return logger;
    }

    /**
     * constains name of resource. The name should be unique for its
     */
    private final String name;


    /**
     * @return name property
     *
     */
    public String getName() {
        return name;
    }

    /**
     * points to the base directory of resource
     */
    private final File baseDir;

    /**
     * @return baseDir
     */
    public File getBaseDir() {
        return baseDir;
    }

    public String toString() {
        return name;
    }

    protected boolean existsSubdir(String name) {
        return getSubdir(name).isDirectory();
    }

    protected File getSubdir(String name) {
        return new File(getBaseDir(), name);
    }

    protected List<File> listSubdirs() {
        File[] values = getBaseDir().listFiles(file ->
                                                       file.isDirectory() &&
                                                       file.getName().matches(VALID_RESOURCE_NAME_REGEX));
        if (values != null) {
            return Stream.of(values).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    protected List<String> listSubdirNames() {
        return listSubdirs().stream().map(File::getName).collect(Collectors.toList());
    }

    protected boolean removeSubDir(String name) {
        if (existsSubdir(name)) {
            return FileUtils.deleteDir(getSubdir(name));
        }
        return false;
    }

}
