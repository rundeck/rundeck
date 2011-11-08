/*
 * Copyright 2010 DTO Labs, Inc. (http://dtolabs.com)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.dtolabs.rundeck.core.utils;

import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterSet;
import org.apache.tools.ant.types.Mapper;

import java.io.File;

/**
 * Takes a set of config file templates that use Ant filter tokens and supplied
 * defaults property file and writes them to the specified configuration directory.
 * <p/>
 * 
 */
public class ConfigWriter {
    final File configDir;
    final File defaultsFile;
    final String includes;
    final String to;

    private ConfigWriter(final File dir, final File propFile, final String includes, final String to) {
        configDir = dir;
        defaultsFile = propFile;
        this.includes = includes;
        this.to = to;
    }
    /**
     * Factory method for creating ConfigWriter instances.
     * @param dir Directory with configuration files and templates
     * @param propFile Property file containing default settings
     * @param includes Glob pattern of file templates to process
     * @param to Glob pattern of generated file
     * @return an instance of ConfigWriter
     */
    public static ConfigWriter create(final File dir, final File propFile, final String includes, final String to) {
        return new ConfigWriter(dir, propFile, includes, to);
    }

    /**
     * Processes the configuration files, filtering templates using defaults info
     * @param overwrite Overwrite existing configuration files with generated ones.
     */
    public void write(final boolean overwrite) {
        final Project project = new Project();
        final Copy copy = new Copy();
        copy.setProject(project);
        copy.setTodir(configDir);
        copy.setFiltering(true);
        copy.setOverwrite(overwrite);
        final FilterSet filterset = copy.createFilterSet();
        filterset.createFiltersfile().setFile(defaultsFile);
        final FileSet fileset = new FileSet();
        fileset.setProject(project);
        fileset.setIncludes(includes);
        fileset.setDir(configDir);
        copy.addFileset(fileset);
        final Mapper mapper = copy.createMapper();
        mapper.setFrom(includes);
        mapper.setTo(to);
        final Mapper.MapperType glob = new Mapper.MapperType();
        glob.setValue("glob");
        mapper.setType(glob);
        copy.execute();
    }
}
