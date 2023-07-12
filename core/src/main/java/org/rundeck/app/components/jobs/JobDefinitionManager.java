/*
 * Copyright 2020 Rundeck, Inc. (http://rundeck.com)
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

package org.rundeck.app.components.jobs;

import com.dtolabs.rundeck.core.plugins.configuration.Validator;

import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public interface JobDefinitionManager<J> {

    /**
     * Decode Job format into a list of Jobs
     *
     * @param reader input XML
     * @return List of jobs
     * @throws JobDefinitionException if a decode error occurs
     */
    List<ImportedJob<J>> decodeFormat(String format, Reader reader) throws JobDefinitionException;

    /**
     * Decode job defintion from a string
     *
     * @param format  job format
     * @param content formatted content
     * @return list of jobs
     */
    List<ImportedJob<J>> decodeFormat(String format, String content) throws JobDefinitionException;

    /**
     * Decode job defintion from a stream
     *
     * @param format      job format
     * @param inputStream input
     * @return list of jobs
     */
    List<ImportedJob<J>> decodeFormat(String format, java.io.InputStream inputStream)
            throws JobDefinitionException;

    /**
     * Decode job defintion from a file
     *
     * @param format job format
     * @param file   yaml content file
     * @return list of jobs
     */
    List<ImportedJob<J>> decodeFormat(String format, File file) throws JobDefinitionException;

    /**
     * Decode YAML from a reader
     *
     * @param reader
     * @return list of jobs
     */
    List<ImportedJob<J>> decodeYaml(Reader reader) throws JobDefinitionException;

    /**
     * Decode YAML job defintion from a file
     *
     * @param file yaml content file
     * @return list of jobs
     */
    List<ImportedJob<J>> decodeYaml(File file) throws JobDefinitionException;

    /**
     * Decode Job XML into a list of Jobs
     *
     * @param reader input XML
     * @return List of jobs
     * @throws JobDefinitionException if a decode error occurs
     */
    List<ImportedJob<J>> decodeXml(Reader reader) throws JobDefinitionException;

    /**
     * Decode Job XML from a file into a list of Jobs
     *
     * @param file file containing XML
     * @return List of jobs
     * @throws JobDefinitionException if a decode error occurs
     */
    List<ImportedJob<J>> decodeXml(File file) throws JobDefinitionException;


    /**
     * Serialize imported list as format
     *
     * @param format  format
     * @param options options
     * @param list    job list
     */
    void exportImportedAs(String format, List<ImportedJob<J>> list, JobFormat.Options options, Writer writer);

    /**
     * Serialize imported list as format
     *
     * @param format format
     * @param list   job list
     */
    void exportImportedAs(String format, List<ImportedJob<J>> list, Writer writer);

    /**
     * Serialize job list as format
     *
     * @param format format
     * @param list   job list
     */
    void exportAs(String format, List<J> list, JobFormat.Options options, Writer writer);

    /**
     * Serialize imported list as format
     *
     * @param format format
     * @param list   job list
     */
    String exportImportedAs(String format, List<ImportedJob<J>> list);

    /**
     * Serialize job list as format
     *
     * @param format format
     * @param list   job list
     */
    void exportAs(String format, List<J> list, Writer writer);

    /**
     * Serialize job list as yaml
     *
     * @param format format
     * @param list   job list
     */
    String exportAs(String format, List<J> list);

    /**
     * Serialize job list as yaml
     *
     * @param list   job list
     */
    String exportAsYaml(List<J> list);

    /**
     * Serialize job list as xml
     *
     * @param list   job list
     */
    String exportAsXml(List<J> list);

    Map jobMapToXMap(Map map, boolean preserveUuid, String replaceId, String stripJobRef);

    /**
     * Validate imported component associations
     * @param importedJob imported job wrapper
     * @return validation report
     */
    Validator.ReportSet validateImportedJob(ImportedJob<J> importedJob);
}
